package com.devsleuth.repository.service;

import com.devsleuth.auth.entity.User;
import com.devsleuth.common.exception.DevSleuthException;
import com.devsleuth.github.service.GitHubRepositoryService;
import com.devsleuth.github.service.GitHubRepositoryService.GitHubRepoInfo;
import com.devsleuth.repository.entity.Repository;
import com.devsleuth.repository.repository.RepositoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RepositoryService {

    private final RepositoryRepository repositoryRepository;
    private final GitHubRepositoryService gitHubRepositoryService;

    public RepositoryService(RepositoryRepository repositoryRepository,
                             GitHubRepositoryService gitHubRepositoryService) {
        this.repositoryRepository = repositoryRepository;
        this.gitHubRepositoryService = gitHubRepositoryService;
    }

    /**
     * Fetches repos from GitHub, syncs to DB, and ensures this user is a member of each.
     * Transactional so the lazy members collection can be read/updated.
     * ponytail: the GitHub list call runs inside the transaction, so a DB connection is
     * held for its duration. Fine at V1 scale; split fetch from persistence if it matters.
     */
    @Transactional
    public List<Repository> listRepositories(User user) {
        List<GitHubRepoInfo> githubRepos = gitHubRepositoryService.listUserRepositories(user.getAccessToken());

        for (GitHubRepoInfo info : githubRepos) {
            Repository repo = repositoryRepository.findByGithubRepositoryId(info.githubId())
                    .orElseGet(() -> {
                        Repository r = new Repository();
                        r.setGithubRepositoryId(info.githubId());
                        r.setOwner(info.owner());
                        r.setName(info.name());
                        r.setFullName(info.fullName());
                        r.setDefaultBranch(info.defaultBranch());
                        r.setLanguage(info.language());
                        r.setConnected(false);
                        return r;
                    });

            boolean alreadyMember = repo.getMembers().stream()
                    .anyMatch(u -> u.getId().equals(user.getId()));
            if (!alreadyMember) {
                repo.getMembers().add(user);
            }
            repositoryRepository.save(repo);
        }

        return repositoryRepository.findByMembers_Id(user.getId());
    }

    /**
     * Mark a repository as connected for analysis. Only members can connect it.
     */
    public Repository connect(UUID repositoryId, User user) {
        Repository repo = repositoryRepository.findByIdAndMembers_Id(repositoryId, user.getId())
                .orElseThrow(() -> new DevSleuthException("Repository not found", HttpStatus.NOT_FOUND));
        repo.setConnected(true);
        return repositoryRepository.save(repo);
    }

    public Optional<Repository> findById(UUID id) {
        return repositoryRepository.findById(id);
    }

    public Optional<Repository> findByFullName(String fullName) {
        return repositoryRepository.findByFullName(fullName);
    }
}

package com.devsleuth.experiment.dto;

import com.devsleuth.common.enums.FindingCategory;
import com.devsleuth.common.enums.Severity;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: pull-requests-and-experiments, Property 5: Invalid experiment input rejected
 *
 * Property 5: For any ExperimentCreateRequest where name is blank, or dataset is empty,
 * or groundTruth is empty, the system SHALL reject with HTTP 400 and SHALL NOT persist an
 * experiment.
 *
 * The 400 rejection is driven by Bean Validation constraints (@NotBlank name,
 * @NotEmpty dataset, @NotEmpty groundTruth) enforced at the controller via @Valid. The
 * ExperimentController POST endpoint does not exist yet (task 5.1), so this property is
 * validated at the constraint layer directly using a jakarta.validation Validator, which
 * is the exact seam that produces the 400. If the validator reports at least one violation,
 * the request never reaches the service and no experiment is persisted.
 *
 * Validates: Requirements 5.2
 */
class ExperimentCreateRequestValidationPropertyTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @Property(tries = 200)
    void invalidExperimentInputIsRejected(@ForAll("invalidRequests") ExperimentCreateRequest request) {
        Set<ConstraintViolation<ExperimentCreateRequest>> violations = VALIDATOR.validate(request);

        // At least one constraint must fire -> controller @Valid would produce HTTP 400,
        // so the request is rejected before any persistence happens.
        assertThat(violations).isNotEmpty();
    }

    @Provide
    Arbitrary<ExperimentCreateRequest> invalidRequests() {
        Arbitrary<String> validNames = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30);
        Arbitrary<String> blankNames = Arbitraries.of("", " ", "   ", "\t", "\n");
        Arbitrary<List<FileChangeDto>> validDatasets =
                validFileChange().list().ofMinSize(1).ofMaxSize(3);
        Arbitrary<List<GroundTruthEntryDto>> validGroundTruth =
                validGroundTruthEntry().list().ofMinSize(1).ofMaxSize(3);

        // At least one of the three fields must be invalidated per generated case.
        Arbitrary<List<Boolean>> invalidationFlags =
                Arbitraries.of(true, false).list().ofSize(3).filter(flags -> flags.contains(true));

        return Combinators.combine(validNames, blankNames, validDatasets, validGroundTruth, invalidationFlags)
                .as((validName, blankName, dataset, groundTruth, flags) -> {
                    boolean invalidateName = flags.get(0);
                    boolean invalidateDataset = flags.get(1);
                    boolean invalidateGroundTruth = flags.get(2);

                    String name = invalidateName ? blankName : validName;
                    List<FileChangeDto> finalDataset = invalidateDataset ? List.of() : dataset;
                    List<GroundTruthEntryDto> finalGroundTruth =
                            invalidateGroundTruth ? List.of() : groundTruth;

                    return new ExperimentCreateRequest(name, "some description", finalDataset, finalGroundTruth);
                });
    }

    private Arbitrary<FileChangeDto> validFileChange() {
        Arbitrary<String> filenames = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(40);
        Arbitrary<String> statuses = Arbitraries.of("added", "modified", "deleted");
        Arbitrary<String> patches = Arbitraries.strings().ofMaxLength(50);
        return Combinators.combine(filenames, statuses, patches).as(FileChangeDto::new);
    }

    private Arbitrary<GroundTruthEntryDto> validGroundTruthEntry() {
        Arbitrary<String> paths = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(40);
        Arbitrary<Integer> lineStarts = intRange(1, 1000);
        Arbitrary<Integer> lineEnds = intRange(1, 1000);
        Arbitrary<FindingCategory> categories = Arbitraries.of(FindingCategory.class);
        Arbitrary<Severity> severities = Arbitraries.of(Severity.class);
        Arbitrary<String> titles = Arbitraries.strings().ofMaxLength(30);
        return Combinators.combine(paths, lineStarts, lineEnds, categories, severities, titles)
                .as(GroundTruthEntryDto::new);
    }

    private Arbitrary<Integer> intRange(int min, int max) {
        return Arbitraries.integers().between(min, max);
    }
}

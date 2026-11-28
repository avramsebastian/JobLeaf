package com.seby.jobleaf.infrastructure.web.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PathsConfigTest {

    private final PathsConfig paths = new PathsConfig();

    @Test
    @DisplayName("Should write path variables as {name}: '${name}' is matched literally, so the rule would never apply")
    void pathVariablesUseSpringSyntax() {
        List<String> all = Stream.of(paths.publicPaths(), paths.adminPaths(), paths.employerPaths(),
                        paths.jobSeekerPaths(), paths.securedPaths(), paths.infrastructurePaths())
                .flatMap(List::stream)
                .toList();

        assertThat(all).noneMatch(path -> path.contains("${"));
    }
}

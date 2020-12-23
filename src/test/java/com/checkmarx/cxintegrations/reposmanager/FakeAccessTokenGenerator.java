package com.checkmarx.cxintegrations.reposmanager;

import com.checkmarx.dto.AccessTokenDto;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import com.checkmarx.dto.github.AccessTokenGithubDto;
import com.checkmarx.dto.gitlab.AccessTokenGitlabDto;
import com.checkmarx.service.ScmService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FakeAccessTokenGenerator {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     *
     * @param scmUrl has the same meaning as {@link ScmService#getBaseDbKey()}
     * @param tokenBody token value to put inside the resulting DTO
     * @return DTO containing a JSON string representing another DTO that containing tokenBody
     * @throws JsonProcessingException when something goes wrong during JSON creation.
     */
    public ScmAccessTokenDto generate(String scmUrl, String tokenBody) throws JsonProcessingException {
        log.info("Generating fake SCM access token DTO. SCM URL: {}, token body: '{}'", scmUrl, tokenBody);
        AccessTokenDto tokenDto;
        if (scmUrl.contains("gitlab")) {
            tokenDto = new AccessTokenGitlabDto();
        } else if (scmUrl.contains("github")) {
            tokenDto = new AccessTokenGithubDto();
        } else {
            final String message = String.format("Unable to create fake SCM access token for the URL: %s", scmUrl);
            throw new NotImplementedException(message);
        }
        tokenDto.setAccessToken(tokenBody);

        String tokenDtoJson = objectMapper.writeValueAsString(tokenDto);
        log.info("SCM access token DTO as JSON: {}", tokenDtoJson);

        return ScmAccessTokenDto.builder()
                .accessToken(tokenDtoJson)
                .scmUrl(scmUrl)
                .build();
    }
}

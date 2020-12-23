Feature: Providing SCM organization config to CxFlow
    In order to perform automated vulnerability scans
    As a CxIntegrations user
    I want CxIntegrations to provide SCM organization config to CxFlow

    # TODO: Add GitLab SCM to scenarios when an issue with its org ID is resolved.

    Scenario Outline: Getting valid CxFlow config
        Given data store contains <team>, <cxgo_secret> and <scm_access_token> for a specific organization in <SCM>
        And the SCM access token is valid for <SCM> API calls
        When CxFlow calls ReposManager API to get this organization config in <SCM> SCM
        Then response status is 200
        And response contains the team field set to <team>
        And response contains the cxgoSecret field set to <cxgo_secret>
        And response contains the scmAccessToken field set to <scm_access_token>
        Examples:
            | team  | cxgo_secret    | scm_access_token  | SCM    |
            | team1 | total-secret-1 | my-valid-gh-token | github |


    Scenario: CxFlow tries to get GitHub organization config when GitHub access token is invalid
    This scenario only deals with GitHub, because GitHub doesn't support refresh tokens.
        Given data store contains my-team, my-cxgo-secret and my-scm-access-token for a specific organization in github
        And GitHub access token is invalid
        When CxFlow calls ReposManager API to get this organization config in github SCM
        Then response status is 417
        And response has the message field containing the text: "token validation failure"


    Scenario Outline: CxFlow tries to get organization config, but ReposManager doesn't have complete organization data
        Given data store contains <team>, <cxgo_secret> and <scm_access_token> for a specific organization in <SCM>
        When CxFlow calls ReposManager API to get this organization config in <SCM> SCM
        Then response status is 417
        And response has the message field containing the text: "missing data"

        Examples:
            | team   | cxgo_secret    | scm_access_token | SCM    |
            | <n/a>  | my-cxgo-secret | my-scm-token     | github |
            | myteam | <n/a>          | my-scm-token     | github |
            | myteam | my-cxgo-secret | <n/a>            | github |


    Scenario Outline: CxFlow tries to get config for an invalid combination of organization and SCM
        Given my-hub-org is the only organization belonging to the github SCM
        And my-lab-org is the only organization belonging to the gitlab SCM
        When CxFlow calls ReposManager API to get configuration for the <org> organization of <SCM> SCM
        Then response status is 404
        And response contains a standard error message

        Examples:
            | SCM             | org             |
            | github          | nonexistent-org |
            | nonexistent-scm | my-hub-org      |
            | github          | my-lab-org      |
            | nonexistent-scm | nonexistent-org |
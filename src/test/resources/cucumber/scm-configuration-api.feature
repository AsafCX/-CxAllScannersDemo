Feature: SCM configuration API
    In order to redirect the user to SCM authorization page
    As a ReposManager API client
    I want to get this SCM configuration

    Scenario Outline: Getting configuration for a valid SCM
        Given data store contains <clientId> and <scope> for the <scm> SCM
        When API client calls the `get configuration` API for the <scm> SCM
        Then response status is 200
        And response contains the clientId field set to <clientId>
        And response contains the scope field set to <scope>
        And response does not have any other fields
        Examples:
            | scm    | clientId         | scope                                   |
            | github | github-client-id | repo,admin:repo_hook,read:org,read:user |
            | gitlab | gitlab-client-id | api                                     |

    Scenario: Trying to get configuration for an invalid SCM
        Given data store does not contain the i-dont-exist SCM
        When API client calls the `get configuration` API for the i-dont-exist SCM
        Then response status is 404
        And response contains the following fields, all non-empty:
            | message       |
            | localDateTime |


Feature: Get Repositories API
    In order to set up automated vulnerability scans
    As a ReposManager API client
    I want to get all repositories for the given organization and their webhooks

  Scenario Outline: Get Repositories API
    When API get repositories is called with scm "<scm>"
    And number of returned repositories will be <n_repos>
    And There are <n_hooks> webhooks defined on the repositories
    And number of active hooks is "<n_active_hooks>"
    And number of hooks with CxFlow url is <n_hooks_with_url>
    And number of hooks with valid events will be <n_hooks_with_events>
    Then repositories list returned by CxIntegration will be <n_repos>
    And number of effective hooks will be <n_effective_hooks>

    Examples:
    
      | scm    | n_repos | n_hooks | n_active_hooks | n_hooks_with_events | n_hooks_with_url | n_effective_hooks |
      | github | 0       | 0       | 0              | 0                   | 0                | 0                 |
      | github | 5       | 0       | 0              | 0                   | 0                | 0                 |
      | github | 5       | 5       | 5              | 5                   | 5                | 5                 |
      | github | 5       | 5       | 4              | 5                   | 3                | 3                 |
      | github | 5       | 4       | 4              | 1                   | 3                | 1                 |
      | gitlab | 0       | 0       | irrelevant     | 0                   | 0                | 0                 |
      | gitlab | 5       | 0       | irrelevant     | 0                   | 0                | 0                 |
      | gitlab | 5       | 5       | irrelevant     | 5                   | 5                | 5                 |
      | gitlab | 5       | 5       | irrelevant     | 4                   | 3                | 3                 |
      | gitlab | 5       | 3       | irrelevant     | 1                   | 3                | 1                 |


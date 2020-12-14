Feature: Repo webhook management API
    In order to set up automated vulnerability scans
    As a ReposManager API client
    I want to create and delete CxIntegrations webhook for a given SCM repo


    Scenario Outline: Creating a webhook
        Given CxIntegrations webhook "<exists or not>" in a <scm> repo
        When API client calls the `create webhook` API for the repo
        Then response status is 200
        And the response contains a non-empty webhook ID
        And CxIntegrations webhook is created in the repo

        Examples:
            | scm    | exists or not |
            | github | doesn't exist |
            | gitlab | doesn't exist |
            # Make sure no error is thrown if the webhook already exists.
            | github | exists        |
            | gitlab | exists        |


    Scenario Outline: Deleting a webhook
        Given CxIntegrations webhook "<exists or not>" in a <scm> repo
        And a third-party webhook exists in the repo
        When API client calls the `delete webhook` API for the repo
        Then response status is 200
        And CxIntegrations webhook is deleted from the repo
        # Make sure ReposManager doesn't delete a wrong webhook.
        But the third-party webhook still exists in the repo

        Examples:
            | scm    | exists or not |
            | github | exists        |
            | gitlab | exists        |
            # Make sure no error is thrown when trying to delete a nonexistent webhook.
            | github | doesn't exist |
            | gitlab | doesn't exist |


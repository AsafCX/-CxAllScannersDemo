Feature: Organization settings API
    In order to perform CxGo scans
    As a ReposManager user
    I want to be able to edit organization settings

    Scenario: Getting settings for a valid organization
        Given data store contains an organization with the myOrgId ID, belonging to the github SCM
        And the myOrgId organization has my-secret-1 CxGo secret and my-team-1 team
        When API client calls the 'get organization settings' API for the myOrgId organization of github SCM
        Then response status is 200
        And response contains the team field set to my-team-1
        And response contains the cxgoToken field set to my-secret-1
        And response does not have any other fields

    Scenario Outline: Trying to get settings for an invalid combination of organization and SCM
        Given my-hub-org is the only organization belonging to the github SCM
        And my-lab-org is the only organization belonging to the gitlab SCM
        When API client calls the 'get organization settings' API for the <org> organization of <scm> SCM
        Then response status is 404
        And response contains a standard error message

        Examples:
            | scm             | org             |
            | github          | nonexistent-org |
            | nonexistent-scm | my-hub-org      |
            | github          | my-lab-org      |
            | nonexistent-scm | nonexistent-org |


    Scenario: Saving settings for an existing organization
        Given data store contains an organization with the myOrgId ID, belonging to the github SCM
        And the myOrgId organization has my-secret-1 CxGo secret and my-team-1 team
        When API client creates a request with cxgoSecret field set to new-secret and team field set to new-team
        And API client calls the 'save organization settings' API for the myOrgId organization of github SCM, using the request above
        Then response status is 200
        And the myOrgId organization now has new-secret CxGo secret and new-team team

    Scenario: Saving settings for an organization that doesn't yet exist
    ReposManager should auto-create an organization in this case, provided the SCM is valid.
        Given data store does not contain any organizations for the github SCM
        When API client creates a request with cxgoSecret field set to new-secret and team field set to new-team
        And API client calls the 'save organization settings' API for the myOrgId organization of github SCM, using the request above
        Then response status is 200
        And data store now contains an organization with the myOrgId ID, belonging to the github SCM
        And the myOrgId organization now has new-secret CxGo secret and new-team team

    Scenario: Trying to save organization settings for an invalid SCM
        Given myAmazingScm SCM does not exist in data store
        When API client calls the 'save organization settings' API for the myAmazingScm SCM
        Then response status is 404
        And response contains a standard error message

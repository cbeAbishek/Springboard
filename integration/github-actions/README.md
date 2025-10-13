# Client GitHub Action Template

Use this workflow template in client repositories to trigger your Automation Platform project whenever code is pushed. The in-app generator now composes the YAML with your project’s endpoint, identifiers, and trigger token.

## Setup Instructions

1. In the Automation Platform dashboard, open your project and expand **CI/CD integration**.
2. Enter the Automation endpoint, remote project identifier (defaults to the internal project ID), token, and triggering branches.
3. Save the integration settings and click **Download workflow file**. The generated YAML includes the values you entered.
4. Commit the downloaded file to the client repository at `.github/workflows/automation-trigger.yml`.
5. Rotate tokens or edit branches any time by updating the integration form and re-downloading.

The workflow issues a `POST /api/run/project/{projectId}` call against the Automation Platform. The platform queues execution of the test suite associated with the provided project identifier.

Ensure the Automation Platform host allows inbound requests from GitHub Actions runners or requires an allow-listed token.

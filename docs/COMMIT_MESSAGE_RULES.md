# Commit Message Guidelines

Make clear, consistent, and review-friendly commit messages. Follow these concise rules used by professional teams.

1. Use Conventional Commits structure
   - Format: `<type>(<scope>): <short summary>`
   - Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `ci`, `build`

2. Keep the subject short and imperative
   - Max ~50 characters. Use present-tense verb: "Add", "Fix", "Update".
   - No trailing period.

3. Use a scope when helpful
   - Optional, short: `api`, `auth`, `ui`, `db`, etc.
   - Example: `fix(auth): validate token expiry`

4. Separate subject from body with a blank line
   - Body explains _why_ and _what_ (not how). Wrap lines at ~72 characters.

5. Reference related issues and PRs in the footer
   - Example: `Closes #123`, `Refs #456`.

6. Indicate breaking changes explicitly
   - Use the footer: `BREAKING CHANGE: <description>` and increment major version.

7. Provide examples
   - `feat(listings): add price history endpoint`
   - `fix(db): add missing index to users.email`
   - `docs: add commit message guidelines`

8. Keep commits focused
   - One logical change per commit. If multiple concerns exist, create separate commits.

9. Tests and CI
   - If the change adds or modifies behavior, include or update tests and mention them: `test(auth): add token refresh tests`.

10. Use automated checks
   - Integrate commit message linting (e.g., `commitlint`) in CI to enforce the style.

Following these rules makes history readable and PRs easier to review. Adopt them in your workflow and CI.

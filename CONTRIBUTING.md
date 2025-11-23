# Contributing

Although this is a personal project, this guide documents development workflow for clarity and professionalism.

## Development Environment
- macOS (Intel or Apple Silicon)
- IntelliJ Community Edition
- Java 21 (Temurin recommended)
- Docker Desktop

## Workflow
1. Create a feature branch  
2. Commit using conventional commit messages  
3. Push to GitHub  
4. Ensure GitHub Actions CI passes  
5. Merge to main  

## Code Style
- Follow standard Java formatting  
- Use meaningful names and comments  
- Ensure tests pass before commit  

## Code Review Checklist
- Confirm new or changed logging follows `docs/LOGGING_STYLE_GUIDE.md` (structured key/value, context keys, level choice, single exception log).  
- Add or update log-capturing tests when logging behavior changes.  
- Validate configuration/documentation updates alongside code so defaults remain safe.  

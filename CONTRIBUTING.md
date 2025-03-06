# Contribution Guidelines for Ticket Express

## Introduction

Welcome to our Ticket Express project! This document outlines the guidelines and procedures for contributing to our repository. By following these guidelines, we'll ensure a smooth collaboration process, maintain code quality, and keep our project organized. These guidelines are designed to help all team members understand how to effectively participate in the development process.

## Getting Started

### Setting Up Your Environment

Before you begin contributing, please ensure you have:

1. Git installed on your local machine
2. A GitHub account connected to our repository
3. Proper access permissions to the repository
4. The necessary development tools for our project (details in the README.md)

### Clone the Repository

```bash
git clone https://github.com/Derftastic/COSC_4P02
cd cosc4p02-project
```

### Creating a Branch

Always create a new branch for your work:

```bash
git checkout -b feature/your-feature-name
```

Name your branches according to the following convention:
- `feature/` - for new features
- `bugfix/` - for bug fixes
- `docs/` - for documentation changes
- `test/` - for adding or modifying tests

## Coding Standards

### General Guidelines

- Write clean, readable, and well-commented code
- Follow the established project architecture
- Maintain consistent indentation and formatting
- Keep functions and methods concise and focused on a single task
- Use meaningful variable and function names that clearly describe their purpose

### Language-Specific Guidelines

Depending on our project's technology stack, adhere to the relevant style guides:
- For JavaScript/TypeScript: Follow the Airbnb JavaScript Style Guide
- For Python: Follow PEP 8
- For Java: Follow Google Java Style Guide

### Documentation

- Document all public API methods and classes
- Include inline comments for complex logic
- Update the README.md when adding new features or changing functionality
- Create usage examples where appropriate

## Commit Process

### Commit Messages

Write clear, concise commit messages that explain what changes were made and why. Follow this format:

```
[Type]: Short summary (50 chars or less)

More detailed explanation if necessary. Wrap text at 
72 characters. Explain the problem that this commit 
is solving and how it solves it.

Fixes #123 (if this closes an issue)
```

Types include:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Formatting, missing semicolons, etc; no code change
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `test`: Adding tests
- `chore`: Updating build tasks, package manager configs, etc.

### Pull Request Process

1. **Create a Pull Request (PR)** from your branch to the main branch
2. **PR Description**: Include a detailed description of the changes, the problem solved, and any implementation details
3. **Link Issues**: Reference any relevant issues using the GitHub issue number (#123)
4. **Request Review**: Assign at least one team member to review your PR
5. **CI/CD**: Ensure all automated tests and builds pass
6. **Review Process**: Address any comments or requested changes from reviewers
7. **Approval**: PRs require approval from at least one team member before merging

## Code Review Guidelines

### For Authors

- Keep PRs small and focused on a single issue or feature
- Provide context and explain your approach in the PR description
- Respond to review comments promptly and respectfully
- Be open to feedback and willing to make changes

### For Reviewers

- Review PRs in a timely manner (within 48 hours if possible)
- Be respectful and constructive in your feedback
- Comment on both what works well and what could be improved
- Focus on code clarity, correctness, and maintainability
- Use questions rather than demands when suggesting changes

## Issue Tracking

- Use GitHub Issues for tracking work
- Create detailed, specific issues with clear acceptance criteria
- Label issues appropriately (bug, enhancement, documentation, etc.)
- Assign priorities and milestone targets when applicable
- When working on an issue, assign it to yourself

## Project Communication

- Use our designated communication channels (Discord server, Microsoft Teams, etc.)
- Post weekly updates on your progress
- Flag blockers or delays as early as possible
- Ask questions and share knowledge openly
- Keep discussions civil and professional

## Testing Guidelines

- Write tests for all new features and bug fixes
- Ensure existing tests pass before submitting PRs
- Follow our test naming conventions
- Aim for reasonable test coverage (at least 70%)
- Include both unit tests and integration tests where appropriate

## Continuous Integration

- All PRs will be automatically tested via our CI pipeline
- Fix failing builds promptly
- Do not merge code that breaks the build

## Academic Integrity

- All work must adhere to the university's academic integrity policies
- Properly cite any external resources or references
- Do not copy code from external sources without proper attribution and permission
- Individual contributions should be clearly identifiable for grading purposes

## Release Process

- We will tag releases according to semantic versioning (MAJOR.MINOR.PATCH)
- Maintain a CHANGELOG.md file with all notable changes
- Prepare release notes for each version
- Test thoroughly before tagging a release

## Getting Help

If you're stuck or unsure about anything, don't hesitate to:
- Ask questions in our group chat
- Bring up issues during our weekly meetings
- Contact the course instructor or TA if necessary
- Leave comments in the code or PR if you need help with a specific section

## Conclusion

These guidelines are meant to help our team work effectively together. They may evolve as our project progresses. Remember that our primary goal is to learn and create quality software as a team. Be patient with yourself and others as we work through this course project together.

Thank you for contributing to our COSC 4P02 project!

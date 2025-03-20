# Contributing

<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#how-to-contribute">How to Contribute</a>
      <ul>
        <li><a href="#reporting-issues">Reporting Issues</a></li>
        <li><a href="#suggesting-features">Suggesting Features</a></li>
        <li><a href="#contributing-code">Contributing Code</a></li>
      </ul>
    </li>
    <li>
      <a href="#development-setup">Development Setup</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li>
            <a href="#development-environment-setup">Development Environment Setup</a>
            <ul>
                <a href="#running-development-servers">Running Development Servers</a>
            </ul>
        </li>
        <li><a href="#recommended-tools">Recommended Tools</a></li>
      </ul>
    </li>
    <li>
        <a href="#style-guidelines">Style Guidelines</a>
        <ul>
            <li><a href="#general">General</a></li>
        </ul>
    </li>
    <li><a href="#pull-request-process">Pull Request Process</a></li>
  </ol>
</details>

## How to Contribute

### Reporting Issues

If you encounter bugs or have ideas for improvements:

1. Search [existing issues](https://github.com/HyScript7/fvBot/issues) to avoid duplicates.

2. If no related issue exists, create one with the following:
   - **Title**: Clear and concise.
   - **Description**: Detailed explanation, including screenshots or logs if relevant.
   - **Reproduction Steps**: Precise steps to reproduce the issue.

### Suggesting Features

To propose a new feature, open a discussion or issue. Provide:
- Use case(s) or scenarios for the feature.
- Why it benefits the project.
- Any relevant examples or references.

### Contributing Code

If you have a solution, patch or implementation relating to a suggested feature or reported issue, you can implement it and [open a pull request](https://github.com/HyScript7/fvBot/compare).

## Development Setup

This section will walk you through how to setup your local development environment for work on this code base.

### Prerequisites

Before you can get started, make sure you have the following installed or prepared:

- Java 21
- Gradle
- a postgres database

  If you don't have a postgres database, you can run one locally using docker:

  ```sh
  docker run -d -p 5432:5432 -e POSTGRES_USER=myuser -e POSTGRES_PASSWORD=mypassword postgres
  ```

### Development Environment Setup

1. Clone the repository using one of the methods below:
    
    ```sh
    git clone git@github.com:HyScript7/fvBot.git
    git clone https://github.com/HyScript7/fvBot.git
    gh repo clone HyScript7/fvBot
    ```

2. Open the project in your preferred IDE

3. Install dependencies and project modules:
    
    ```sh
    cd backend/
    pip install -r requirements.txt
    cd ../frontend/
    pnpm install
    ```

#### Running Development Servers

You can use this gradle command to run the bot without building a jar or docker image:

  ```sh
  gradle bootrun
  ```

### Recommended Tools

You can install these optional tools to help with development:

- Docker - A tool for running and managing containers, building images

## Style Guidelines

To keep style across the codebase somewhat consistent, you can find guidelines for individual sections below. Please attempt to follow them. If you don't, but your code is understandable and readable, you can go without these.<!-- Kinda ironic considering most of what I write is comparable to hieroglyphs -->

### General

    Use meaningful commit messages (e.g., fix: resolve issue with X or feat: add Y functionality).

    Write tests for new features or fixes if you feel like it.

## Pull Request Process

1. Fork the repo and create your branch:
  ```
  git checkout -b feature/your-feature
  ```

2. Make your changes and ensure:
  Linting passes (if there is any).
  That all relevant tests pass (if there are any).

3. Push to your fork and open a pull request:
  Describe your changes in detail.
  Link relevant issues or discussions.

4. Respond to feedback and make updates as needed.

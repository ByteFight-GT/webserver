# Database Schema and Workflow

This project uses **DBML for schema design** and **Flyway for schema migrations**.

The goal is to get the best of both worlds:

- DBML = human-readable schema source of truth
- Flyway = deterministic, versioned database changes
- Git = full audit history of both design and execution

This document explains how to safely make database changes without causing schema drift.


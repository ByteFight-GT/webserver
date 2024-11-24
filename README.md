
# Setup
HIGHLY RECOMMENDED: Use Intellij. The information in this README assumes you are using IntelliJ. it does not matter if you have IntelliJ ultimate.

Must have Java 17 installed and set in project. To do so:
```
file > Project Structure > Project > SDK
```
Also set the Language level to SDK default. If you have OpenJDK 17 from Oracle, use that. Else, you can download Amazon Corretto 7.0.13 from Intellij Directly if you click Download JDK in SDK

# Making Changes

To make changes create a branch with a name corresponding to the feature you are developing. For example, the branch corresponding to updating the ranking system to use glicko rather than elo was named 
```
glicko_branch
```
Of course, the fact that this is a branch is implied, and you probably don't need to specify that in the branch name. 

Once you are done making your changes, create a pull request merging into main. This should trigger GitHub actions that verify your code passes the necessary tests before allowing you to merge. Send your PR to infrastructure lead (currently Tyler) for review. Once your code passes the CI checks, and is approved by the lead, merge your code.

Working on this repository should NOT require production database or storage access. Instead, verify that your changes work via unit/integration tests provided. When working on a specific component dependent on other components, it should be assumed that the other components work as intended. For this reason, when testing, use Mocks.

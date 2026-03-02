# Convention Plugins

Custom plugins to standardize build logic.

## Manifest Permission Validation

Validates that all `AndroidManifest.xml` permissions match a baseline file, detecting unintended
changes from dependencies, tracking changes over time and documenting their implications.

### Architecture

#### Components

1. **ManifestPermissionValidationPlugin**: Entry point to register tasks to variants
2. **GenerateManifestBaselineTask**: Generates a baseline file based on the merged manifest
3. **CheckManifestPermissionsTask**: Compares the merged manifest against the baseline
4. **PermissionsBaselineParser**: Utility for parsing and writing to files

#### Workflow

```
Source AndroidManifest.xml + dependencies
         ↓
   AGP Manifest Merger
         ↓
Merged AndroidManifest.xml ←→ baseline.xml (version control)
         ↓
CheckManifestPermissionsTask
         ↓
   Pass / Fail
```

### Usage

To generate the initial baseline, or update it when intentionally modifying permissions run:

```bash
./gradlew generateReleaseManifestBaseline
```

Then commit the resulting file to source control. To verify against the generated baseline run:

```bash
./gradlew checkReleaseManifestPermissions
```

This task is also automatically hooked to the `check` task.

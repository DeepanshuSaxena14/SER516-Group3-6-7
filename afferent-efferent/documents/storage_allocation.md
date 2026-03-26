# Storage Allocation

this project uses a dedicated storage structure to support cloning,temp repo,processed metric output,raw snapshots and logs.

# Folder Structure

- storage/raw/ - raw data snapshots
- storage/processed/ -  processed metric output
- storage/logs/ -  runtime logs
- storage/clones/ - cloned repositories for analysis
- storage/temp/ - temp repo used for metadata lookup

# Configuration

By default the storage root -

storage/

This can be overridden using the environment variable -

STORAGE_ROOT
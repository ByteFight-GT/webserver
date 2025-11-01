#!/bin/bash
if [ -z "$DB_HOST" ] || [ -z "$DB_PORT" ] || [ -z "$DB_NAME" ] || [ -z "$DB_PASSWORD" ] || [ -z "$DB_USER" ]; then
  echo "Error: one or more of the required variables (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD were not found in the .env file."
  exit 1
fi

echo "Starting backup process"

mkdir -p "$BACKUP_DIR"
if [ $? -ne 0 ]; then
  echo "Error: could not create backup directory '$BACKUP_DIR'."
  exit 1
fi

echo "Connection details:"
echo "  - Host: $DB_HOST"
echo "  - Port: $DB_PORT"
echo "  - Database: $DB_NAME"
echo "  - User: $DB_USER"

TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")

BACKUP_FILE="$BACKUP_DIR/dump_${DB_NAME}_${TIMESTAMP}.dump"

export PGPASSWORD="$DB_PASSWORD"

echo "creating dump for database '$DB_NAME'"
pg_dump -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        -F c \
        -f "$BACKUP_FILE"

EXIT_CODE=$?

unset PGPASSWORD

if [ $EXIT_CODE -ne 0 ]; then
  echo "---"
  echo "pg_dump failed with exit code $EXIT_CODE."
  rm "$BACKUP_FILE" 2>/dev/null
  exit 1
fi


echo "Database backup created successfully"
echo "File: $BACKUP_FILE"

exit 0





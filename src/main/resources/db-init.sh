#!/bin/bash

# Database initialization script for Inkwell

# Variables
DB_NAME="inkwell"
DB_USER="postgres"
DB_PASSWORD="postgres"

# Create database
echo "Creating database $DB_NAME..."
PGPASSWORD=$DB_PASSWORD psql -U $DB_USER -c "DROP DATABASE IF EXISTS $DB_NAME;"
PGPASSWORD=$DB_PASSWORD psql -U $DB_USER -c "CREATE DATABASE $DB_NAME;"

# Apply schema
echo "Applying database schema..."
PGPASSWORD=$DB_PASSWORD psql -U $DB_USER -d $DB_NAME -f schema.sql

# Load sample data (optional)
echo "Loading sample data..."
PGPASSWORD=$DB_PASSWORD psql -U $DB_USER -d $DB_NAME -f data.sql

echo "Database initialization complete!"


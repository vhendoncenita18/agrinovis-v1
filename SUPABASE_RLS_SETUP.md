# Supabase Row Level Security (RLS) Setup for Agrinovis

To ensure strict data privacy and isolation between users (Requirement 7: **A user must NEVER see another user's plants**), PostgreSQL Row Level Security (RLS) must be enabled on the Supabase `plants` table.

Execute the following SQL commands in your Supabase SQL Editor:

```sql
-- 1. Enable Row Level Security on the plants table
ALTER TABLE plants ENABLE ROW LEVEL SECURITY;

-- 2. Policy for SELECT: Users can only read their own plants
CREATE POLICY "Users can view their own plants" ON plants
    FOR SELECT 
    USING (auth.uid() = user_id);

-- 3. Policy for INSERT: Users can only insert plants belonging to themselves
CREATE POLICY "Users can insert their own plants" ON plants
    FOR INSERT 
    WITH CHECK (auth.uid() = user_id);

-- 4. Policy for UPDATE: Users can only update plants belonging to themselves
CREATE POLICY "Users can update their own plants" ON plants
    FOR UPDATE 
    USING (auth.uid() = user_id) 
    WITH CHECK (auth.uid() = user_id);

-- 5. Policy for DELETE: Users can only delete plants belonging to themselves
CREATE POLICY "Users can delete their own plants" ON plants
    FOR DELETE 
    USING (auth.uid() = user_id);
```

## Explanation
- **`auth.uid()`**: Refers to the authenticated user's unique identifier provided by Supabase Auth.
- **`user_id`**: The column in the `plants` table storing the owner's user identifier.
- **Enforcement**: The database itself enforces these restrictions, ensuring that even if application-level queries fail or filters are bypassed, unauthorized cross-user data access is blocked at the database level.

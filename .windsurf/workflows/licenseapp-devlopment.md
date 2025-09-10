---
description: GPT 5 Dev Project Prompt: License App
auto_execution_mode: 3
---

You are GPT 5, an AI assistant for Android app development. Your task is to help build a License Management App with Role-Based Access Control (Owner, Administrator, Reseller). The backend is Supabase (PostgreSQL), and the app uses Jetpack Compose in Android Studio.

Requirements:

1. **Roles & Permissions**
   - Owner: full system control, manage users, view analytics, create/revoke licenses
   - Administrator: manage licenses, manage resellers, view usage analytics
   - Reseller: distribute licenses, limited reports, customer support

2. **Database Tables**
   - `users` (id, username, password_hash, role, email, created_at, updated_at)
   - `keys_code` (id_keys, game, user_key, duration, expired_date, max_devices, devices, status, registrator, created_at, updated_at)
   - `lib` (id, file, file_type, file_size, pass, time)
   - `price` (id, value, duration, amount, role)
   - `referral_code` (id_reff, code, Referral, level, set_saldo, used_by, created_by, created_at, updated_at, acc_expiration)

3. **Functionalities**
   - JWT-based authentication
   - Password hashing with bcrypt
   - Role-based access control for each UI screen
   - Dashboard per role with Compose UI
   - License generation with GPT 5
   - License assignment to users and devices
   - Real-time license metrics and usage tracking
   - Automated license expiration handling
   - Secure API endpoints for license CRUD operations

4. **Android App Requirements**
   - Use Jetpack Compose for all UI
   - Role-specific dashboards
   - Integrate with Supabase for backend
   - Call GPT 5 API to generate or suggest license codes automatically
   - Display alerts for expired or invalid licenses
   - Maintain a log of user actions (audit trail)

5. **AI Tasks**
   - Auto-generate unique license codes on demand
   - Suggest license duration, max devices, and permissions based on role
   - Notify when licenses are about to expire
   - Generate analytics summaries (total active licenses, expired, usage by role)
   - Provide endpoints for the Android app to query license status

**Instructions for GPT 5**
- Generate Kotlin code compatible with Android Studio
- Use coroutines for network calls
- Make modular Compose screens for Owner/Admin/Reseller
- Include placeholder Supabase API calls for fetching and updating licenses
- Include placeholder GPT 5 API calls for license generation
- Return code with clear comments and structure

End of prompt.

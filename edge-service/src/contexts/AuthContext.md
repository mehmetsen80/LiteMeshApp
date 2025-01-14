# Authentication Flow

## Overview
The application uses a context-based authentication system with JWT tokens.

## Components
1. **AuthContext**: Central auth state management
2. **AuthProvider**: Wraps the app and provides auth functionality
3. **useAuth**: Custom hook for accessing auth context
4. **ProtectedRoute**: Route wrapper for authenticated routes

## Flow
1. User attempts to access protected route
2. ProtectedRoute checks for user in AuthContext
3. If no user, redirects to login
4. On login:
   - Credentials sent to API
   - JWT token received
   - User data stored in context and localStorage
   - Redirect to protected route

## Protected Routes
- /
- /metrics
- /service-status
- /alerts

## Public Routes
- /login
- /register 
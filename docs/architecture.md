# Architecture

Gateway routes /auth → Auth, /properties → Property, /favourites|/interests|/activity → Interaction.
Shared MySQL hestia_db with logical ownership:
- Auth: users
- Property: properties, property_images
- Interaction: favourites, interests, activity_history
JWT verified in each service. ADMIN role required for mutations.

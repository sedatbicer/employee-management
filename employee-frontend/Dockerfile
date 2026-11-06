# Stage 1: Angular Build
FROM node:24-alpine AS build

WORKDIR /app

COPY package*.json ./

RUN npm ci

COPY . .

RUN npm run build -- --configuration production


# Stage 2: Nginx
FROM nginx:alpine

COPY --from=build /app/dist/employee-management/browser /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
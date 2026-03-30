FROM nginx:alpine

# Copy frontend static files into nginx web root
COPY . /usr/share/nginx/html

# Copy their custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf
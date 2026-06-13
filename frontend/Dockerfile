# Imagen base: Nginx (servidor web)
FROM nginx:alpine

# Borrar la configuracion por defecto de Nginx
RUN rm /etc/nginx/conf.d/default.conf

# Copiar nuestra configuracion personalizada
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar la app React compilada a la carpeta que Nginx sirve
COPY dist /usr/share/nginx/html

EXPOSE 80
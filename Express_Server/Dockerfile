FROM node:alpine


RUN mkdir -p /var/www/app


WORKDIR /var/www/app

COPY package.json /var/www/app

RUN npm install

RUN npm install nodemon

EXPOSE 3000

CMD ["npm", "start"]

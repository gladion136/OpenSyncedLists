/**
 * Sensorverwaltung Backend
 */

import cors from "cors";
import express from "express";
import path from "path";
import { listRouter } from "./route/list";
import { MongoDBGateway } from "./util/storage/mongodb-gateway";

const DEBUG = true;

const app = express();
const port = 3000;
app.use(cors());
app.use(express.json());

/**
 * Initilize DB
 */
export const db = new MongoDBGateway();
const initDB = async () => {
    // tslint:disable-next-line:no-console
    console.log("open DB");
    await db.open_DB();
    // tslint:disable-next-line:no-console
    console.log("Initilize DB");
    // tslint:disable-next-line:no-console
    console.log(await db.init_DB());
};
initDB();
/**
 * Handle routes
 */
app.get("/test", (req, res) => {
    res.send('{"status":"OK"}');
});

app.use("/list", listRouter);

app.get("/", (req, res) => {
    res.sendFile(path.join(__dirname, "/html/index.html"));
});

app.listen(port, () => {
    // tslint:disable-next-line:no-console
    return console.log(`server is listening on ${port}`);
});

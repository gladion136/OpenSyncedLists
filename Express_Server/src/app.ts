/**
 * Sensorverwaltung Backend
 */
import cors from "cors";
import express from "express";
import { listRouter } from "./route/list";
import { MongoDBGateway } from "./util/storage/mongodb-gateway";

const DEBUG = true;

const app = express();
const port = 3000;
app.use(cors());
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
    res.send("OK");
});

app.use("/list", listRouter);

app.use("/", (req, res) => {
    res.send("Please use the OpenSyncedLists App!");
});

app.listen(port, () => {
    // tslint:disable-next-line:no-console
    return console.log(`server is listening on ${port}`);
});

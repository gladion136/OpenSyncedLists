/**
 * API für die Settings
 */
import * as express from "express";
import { db } from "../app";
import { IList } from "../util/structures/list";

export const listRouter = express.Router();

listRouter.get("/test", (req, res) => {
    res.send('{"status":"OK"}');
});

listRouter.get("/get", async (req, res) => {
    if (
        req.query == null ||
        req.query.id === undefined ||
        req.query.secret === undefined
    ) {
        res.send("Error wrong paramenters");
        return;
    } else {
        res.send(
            await db
                .get_list(String(req.query.id), String(req.query.secret))
                .then((resp) => {
                    res.send(resp);
                })
                .catch((err) => {
                    res.send(err);
                })
        );
    }
});

listRouter.get("/set", async (req, res) => {
    if (req.query == null || req.query.list == null) {
        res.send("Error wrong paramenters");
        return;
    }

    const list = req.query.list as unknown as IList;
    if (list.id && list.secret) {
        await db
            .set_list(list)
            .then((resp) => {
                res.send(resp);
            })
            .catch((err) => {
                res.send(err);
            });
    }

    res.send("Wrong list");
});

listRouter.get("/add", async (req, res) => {
    if (req.query == null || req.query.list === undefined) {
        res.send("Error wrong paramenters");
        return;
    } else {
        const list = req.query.list as unknown as IList;
        if (list.id && list.secret) {
            await db
                .add_list(list)
                .then((resp) => {
                    res.send(resp);
                })
                .catch((err) => {
                    res.send(err);
                });
        }
    }
});

listRouter.get("/remove", async (req, res) => {
    if (
        req.query == null ||
        req.query.id === undefined ||
        req.query.secret === undefined
    ) {
        res.send("Error wrong paramenters");
        return;
    } else {
        res.send(
            await db
                .delete_list(String(req.query.id), String(req.query.secret))
                .then((resp) => {
                    res.send(resp);
                })
                .catch((err) => {
                    res.send(err);
                })
        );
    }
});

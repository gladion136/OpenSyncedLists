/**
 * API für die Settings
 */
import * as express from "express";
import path from "path";
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
        res.send('{"status":"ERROR", "msg":"Wrong parameters"}');
        return;
    } else {
        res.send(
            await db
                .get_list(req.query.id as string, req.query.secret as string)
                .then((resp) => {
                    res.send({
                        msg: resp,
                        status: "OK",
                    });
                })
                .catch((err) => {
                    res.send({
                        msg: err,
                        status: "ERROR",
                    });
                })
        );
    }
});

listRouter.post("/set", async (req, res) => {
    if (
        req.query == null ||
        req.query.id === undefined ||
        req.query.secret === undefined ||
        req.body === undefined ||
        req.body.data === undefined ||
        req.body.basedOnHash === undefined ||
        req.body.hash === undefined
    ) {
        res.send('{"status":"ERROR", "msg":"Wrong parameters"}');
        return;
    }

    const list: IList = {
        data: req.body.data,
        hash: req.body.hash,
        id: req.query.id as string,
        secret: req.query.secret as string,
    };
    if (list.id && list.secret) {
        await db
            .set_list(list, req.body.basedOnHash as string)
            .then((resp) => {
                res.send({
                    msg: resp,
                    status: "OK",
                });
            })
            .catch((err) => {
                res.send({
                    msg: err,
                    status: "ERROR",
                });
            });
    }
});

listRouter.post("/add", async (req, res) => {
    if (
        req.query == null ||
        req.query.id === undefined ||
        req.query.secret === undefined ||
        req.body === undefined ||
        req.body.data === undefined ||
        req.body.hash === undefined
    ) {
        res.send('{"status":"ERROR", "msg":"Wrong parameters"}');
        return;
    } else {
        const list: IList = {
            data: req.body.data,
            hash: req.body.hash,
            id: req.query.id as string,
            secret: req.query.secret as string,
        };
        if (list.id && list.secret) {
            await db
                .add_list(list)
                .then((resp) => {
                    res.send({
                        msg: resp,
                        status: "OK",
                    });
                })
                .catch((err) => {
                    res.send({
                        msg: err,
                        status: "ERROR",
                    });
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
        res.send('{"status":"ERROR", "msg":"Wrong parameters"}');
        return;
    } else {
        res.send(
            await db
                .delete_list(String(req.query.id), String(req.query.secret))
                .then((resp) => {
                    res.send({
                        msg: resp,
                        status: "OK",
                    });
                })
                .catch((err) => {
                    res.send({
                        msg: err,
                        status: "ERROR",
                    });
                })
        );
    }
});

listRouter.get("/share", (req, res) => {
    res.sendFile(path.join(__dirname, "../html/share.html"));
});

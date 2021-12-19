import { MongoClient } from "mongodb";
import { rejects, strict } from "node:assert";
import { IList } from "../structures/list";
import { IDBGateway } from "./db-gateway";

/**
 * Verbindung zu einer MongoDB Datenbank
 */
export class MongoDBGateway implements IDBGateway {
    public url = "mongodb://database:27017";
    public client;

    public errorPromise = new Promise(async (resolve, reject) => {
        resolve("DB error");
    });

    public async;

    public async open_DB() {
        this.client = await MongoClient.connect(this.url);
    }

    public async init_DB() {
        let callback = "";

        await new Promise(async (resolve, reject) => {
            const dbo = this.client.db("mydb");
            await dbo.createCollection("lists", async (error, res) => {
                if (error) {
                    reject("Lists collection already created\n");
                } else {
                    resolve("Lists collection created!\n");
                }
            });
        })
            .then((res) => {
                callback += res;
            })
            .catch((err) => {
                callback += err;
            });
        return callback;
    }

    public async add_list(list: IList) {
        if (this.client === undefined) {
            return this.errorPromise;
        }
        // tslint:disable-next-line:no-console
        console.log("Add:" + list.id);
        const dbo = this.client.db("mydb");
        const query = { id: list.id, secret: list.secret };
        return new Promise(async (resolve, reject) => {
            dbo.collection("lists")
                .find(query)
                .toArray((error, result) => {
                    if (error || result.length <= 0) {
                        // tslint:disable-next-line:no-console
                        console.log("List: " + list.id);
                        dbo.collection("lists").insertOne(list, (err, res) => {
                            if (err) {
                                reject(
                                    "register new list failed: " + String(err)
                                );
                            } else {
                                resolve("register new list success");
                            }
                        });
                    } else {
                        reject("list already exists");
                    }
                });
        });
    }

    public async set_list(list: IList, oldHash: string) {
        if (this.client === undefined) {
            return this.errorPromise;
        }
        const dbo = this.client.db("mydb");
        const query = { id: list.id, secret: list.secret, hash: oldHash };
        return new Promise(async (resolve, reject) => {
            await dbo
                .collection("lists")
                .updateOne(query, { $set: list }, (err, result) => {
                    if (err || !result) {
                        reject("Error while update");
                    } else {
                        resolve("Success");
                    }
                });
        });
    }

    public async get_list(id: string, secret: string) {
        if (this.client === undefined) {
            return this.errorPromise;
        }
        const dbo = this.client.db("mydb");
        const query = { id, secret };

        return await new Promise(async (resolve, reject) => {
            const collection = await dbo.collection("lists");
            collection.findOne(
                query,
                { projection: { id: 1, secret: 1, data: 1 } },
                (err, result) => {
                    if (err || !result) {
                        err ? reject(err) : reject("Not found");
                    } else {
                        resolve(result);
                    }
                }
            );
        });
    }

    public async delete_list(id: string, secret: string) {
        if (this.client === undefined) {
            return this.errorPromise;
        }
        const dbo = this.client.db("mydb");
        const query = { id, secret };
        return new Promise(async (resolve, reject) => {
            await dbo.collection("lists").deleteOne(query, (err, result) => {
                if (err) {
                    reject("Error while removing");
                } else {
                    resolve("Success");
                }
            });
        });
    }
}

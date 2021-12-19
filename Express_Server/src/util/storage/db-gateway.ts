import { IList } from "../structures/list";

/**
 * Interface einer Speicherschnittstelle
 */
export interface IDBGateway {
    /**
     * Datenbank verbindung herstellen und testen
     */
    open_DB();

    /**
     * Initilisiere die Datenbank (Tabellen erstellen etc.)
     */
    init_DB();

    /**
     * Add list to db
     *
     * @param list List
     */
    add_list(list: IList);

    /**
     * Set list in db (same id)
     *
     * @param list List
     */
    set_list(list: IList, oldHash: string);

    /**
     * Get list from db
     *
     * @param id listId
     * @param secret secret (HASH)
     */
    get_list(id: string, secret: string);

    /**
     * Delete list from db
     *
     * @param id listId
     * @param secret secret (HASH)
     */
    delete_list(id: string, secret: string);
}

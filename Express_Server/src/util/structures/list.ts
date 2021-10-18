export interface IListStep {
    id: number; // Stepid
    timestamp: number | null;
    changeId: string | null; // Id of element
    changeAction: number | null; // enum
    changeValues: JSON | null; // new values
}

export interface IList {
    id: string;
    name: string;
    secret: string;
    elementSteps: IListStep[] | null;
}

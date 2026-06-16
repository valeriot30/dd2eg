package com.dd2eg.backend.tasks.events;

public enum EventStatus {
    PENDING,     // da processare
    COMPLETED,   // sincronizzato su Neo4j con successo
    FAILED       // superato N_MAX tentativi
}

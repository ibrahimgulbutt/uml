package org.example.scdpro2.business.models.BPackageDiagarm;

import org.example.scdpro2.business.models.Diagram;
import org.example.scdpro2.business.models.DiagramType;
import org.example.scdpro2.business.models.BClassDiagarm.Relationship;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PackageComponent extends Diagram implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id; // Unique ID
    private String name;
    private final List<Relationship> relationships;
    private ArrayList<PackageClassComponent> packageClassComponents;

    public PackageComponent(String name) {
        super(name);
        this.id = UUID.randomUUID().toString(); // Generate a unique ID
        this.name = name;
        this.relationships = new ArrayList<>();
    }


    public String getId() {
        return id; // Return the unique ID
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addRelationship(Relationship relationship) {
        relationships.add(relationship);
    }

    public void removeRelationship(Relationship relationship) {
        relationships.remove(relationship);
    }

    public List<Relationship> getRelationships() {
        return relationships;
    }

    public String generateCode() {
        StringBuilder codeBuilder = new StringBuilder("package " + name + " {\n");
        for (Relationship relationship : relationships) {
            codeBuilder.append("// Relationship: ").append(relationship).append("\n");
        }
        codeBuilder.append("}");
        return codeBuilder.toString();
    }

    @Override
    public void render() {
        // UI rendering logic, if needed
    }

    @Override
    public String toCode() {
        return generateCode();
    }

    @Override
    public DiagramType getType() {
        return DiagramType.PACKAGE;
    }

    public void addClassBox(PackageClassComponent newPackage) {
        this.packageClassComponents.add(newPackage);
    }
}
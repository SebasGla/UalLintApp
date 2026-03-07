package app;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/*
 * Data model for the custom hardware registers table in the GUI.
 * Uses JavaFX StringProperty so the TableView can automatically
 * track changes and update when the user edits a cell.
 */
public final class RegisterEntry
{
    private final StringProperty registerName;
    private final StringProperty address;

    public RegisterEntry(String registerName, String address)
    {
        this.registerName = new SimpleStringProperty(registerName);
        this.address = new SimpleStringProperty(address);
    }

    public String getRegisterName()
    {
        return registerName.get();
    }

    public void setRegisterName(String v)
    {
        registerName.set(v);
    }

    /*
     * Required by JavaFX PropertyValueFactory to bind the name data to the UI table column.
     */
    public StringProperty registerNameProperty()
    {
        return registerName;
    }

    public String getAddress()
    {
        return address.get();
    }

    public void setAddress(String v)
    {
        address.set(v);
    }

    /*
     * Required by JavaFX PropertyValueFactory to bind the address data to the UI table column.
     */
    public StringProperty addressProperty()
    {
        return address;
    }
}
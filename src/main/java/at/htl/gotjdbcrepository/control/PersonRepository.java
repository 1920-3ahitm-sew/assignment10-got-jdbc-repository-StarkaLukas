package at.htl.gotjdbcrepository.control;
import at.htl.gotjdbcrepository.entity.Person;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PersonRepository implements Repository {
    public static final String USERNAME = "app";
    public static final String PASSWORD = "app";
    public static final String DATABASE = "db";
    public static final String URL = "jdbc:derby://localhost:1527/" + DATABASE + ";create=true";
    public static final String TABLE_NAME = "person";

    private static PersonRepository instance;

    private PersonRepository() {
    }

    public static synchronized PersonRepository getInstance() {
        if (instance == null) {
            instance = new PersonRepository();
        }
        instance.createTable();
        return instance;
    }

    private void createTable() {
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            try (Statement stmt = conn.createStatement()) {
                String sql = "CREATE TABLE " + TABLE_NAME + " (" +
                        "id INT NOT NULL GENERATED ALWAYS AS IDENTITY CONSTRAINT " + TABLE_NAME + "_pk PRIMARY KEY," +
                        "name VARCHAR(255)," +
                        "city VARCHAR(255)," +
                        "house VARCHAR(255)," +
                        "CONSTRAINT " + TABLE_NAME + "_uq UNIQUE (name, city, house)" +
                        ")";
                stmt.executeUpdate(sql);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            //System.err.format("SQL State: %s - %s\n", e.getSQLState(), e.getMessage());
        }
    }

    public void deleteAll() {
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            try (Statement stmt = conn.createStatement()) {
                String sql = "DELETE FROM " + TABLE_NAME;
                stmt.executeUpdate(sql);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Hat newPerson eine ID (id != null) so in der Tabelle die entsprechende Person gesucht und upgedated
     * Hat newPerson keine ID wird ein neuer Datensatz eingefügt.
     * <p>
     * Wie man die generierte ID zurück erhält: https://stackoverflow.com/a/1915197
     * <p>
     * Falls ein Fehler auftritt, wird nur die Fehlermeldung ausgegeben, der Programmlauf nicht abgebrochen
     * <p>
     * Verwenden sie hier die privaten MEthoden update() und insert()
     *
     * @param newPerson
     * @return die gespeicherte Person mit der (neuen) id
     */
    @Override
    public Person save(Person newPerson) {
        if (newPerson.getId() != null) {
            if (update(newPerson) == 1) {
                return newPerson;
            } else {
                System.err.println("Could not update person");
                return insert(newPerson);
            }
        } else {
            return insert(newPerson);
        }
    }

    /**
     * Wie man die generierte ID erhält: https://stackoverflow.com/a/1915197
     *
     * @param personToSave
     * @return Rückgabe der Person inklusive der neu generierten ID
     */
    private Person insert(Person personToSave) {
        String sql = "INSERT INTO " + TABLE_NAME + " (NAME, CITY, HOUSE) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, personToSave.getName());
            preparedStatement.setString(2, personToSave.getCity());
            preparedStatement.setString(3, personToSave.getHouse());

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                System.err.println("Creating person failed, no rows affected.");
            }

            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet.next()) {
                personToSave.setId(resultSet.getLong(1));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return personToSave;
    }

    /**
     * @param personToSave
     * @return wenn erfolgreich --> Anzahl der eingefügten Zeilen, also 1
     * wenn nicht erfolgreich --> -1
     */
    private int update(Person personToSave) {
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            String sql = "UPDATE " + TABLE_NAME + " set name=?, city=?, house=? where id = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, personToSave.getName());
            preparedStatement.setString(2, personToSave.getHouse());
            preparedStatement.setString(3, personToSave.getCity());
            preparedStatement.setLong(4, personToSave.getId());

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows != 1) {
                System.err.println("Updating person was not successful");
                return -1;
            } else {
                return affectedRows;
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return -1;
    }

    @Override
    public void delete(long id) {
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            String sql = "DELETE FROM " + TABLE_NAME + " where id=?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setLong(1, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Finden Sie eine Person anhand Ihrer ID
     *
     * @param id
     * @return die gefundene Person oder wenn nicht gefunden wird null zurückgegeben
     */
    public Person find(long id) {
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            String sql = "SELECT id, name, city, house FROM " + TABLE_NAME + " where id=?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setLong(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    Person person = new Person(resultSet.getString("name"), resultSet.getString("city"), resultSet.getString("house"));
                    person.setId(resultSet.getLong("id"));
                    return person;
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    /**
     * @param house Name des Hauses
     * @return Liste aller Personen des gegebenen Hauses
     */
    public List<Person> findByHouse(String house) {
        List<Person> people = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            String sql = "SELECT id, name, city, house FROM person where house=?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);

            preparedStatement.setString(1, house);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Person person = new Person(resultSet.getString("name"), resultSet.getString("city"), resultSet.getString("house"));
                person.setId(resultSet.getLong("id"));
                people.add(person);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return people;
    }


}

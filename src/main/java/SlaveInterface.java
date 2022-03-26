import annotation.ForeignFunction;
import marker.ForeignService;
import model.AddressBook;
import model.Person;

import java.util.concurrent.Future;

/**
 * The interface Slave interface.
 */
public interface SlaveInterface extends ForeignService {

    /**
     * Gets async person.
     *
     * @param addressBook the address book
     * @return the async person
     */
    @ForeignFunction(functionSignature = "getAsyncPerson")
    Future<Person> getAsyncPerson(AddressBook addressBook);

    /**
     * Gets person.
     *
     * @param addressBook the address book
     * @return the person
     */
    @ForeignFunction(functionSignature = "getPerson")
    Person getPerson(AddressBook addressBook);

}

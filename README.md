# Instructions for candidates

This is the Java version of the Payment Gateway challenge. If you haven't already read this [README.md](https://github.com/cko-recruitment/) on the details of this exercise, please do so now.

## Requirements
- JDK 17
- Docker

## Template structure

src/ - A skeleton SpringBoot Application

test/ - Some simple JUnit tests

imposters/ - contains the bank simulator configuration. Don't change this

.editorconfig - don't change this. It ensures a consistent set of rules for submissions when reformatting code

docker-compose.yml - configures the bank simulator


## API Documentation
For documentation openAPI is included, and it can be found under the following url: **http://localhost:8090/swagger-ui/index.html**

**Feel free to change the structure of the solution, use a different library etc.**

## Considerations and assumptions
The solution design follows the provided project structure (Controller → Service → Repository) which is well known and thus easy to understand for most developers.
#### API
Api descriptions include summaries and valid examples.  This makes for easy use for any users or agents.

Post payment endpoint: use POST because creating a resource is not idempotent and has side effects. PUT is more for specific resource updates.

Post payment endpoint: For fields expiry_month and expiry_year choose that the current month + year is valid (different than in the requirements) because on bank cards the expiry date is at the end of the month. Being new at the job I would double check with someone

Post payment endpoint:  For the field amount, the type Integer is used instead of int. Additionally, ACCEPT_FLOAT_AS_INT is disabled, otherwise floats are truncated to integers which leads to unwanted behaviour. The field amount also accepts only values greater than 0 because payments with 0 amount are no-ops. The max value is integer max for now but there is probably a legal max value which i would use in a real setting.

Post payment endpoint: Within the request object the field CardNumberLastFour is not needed and thus removed. CardNumber is used instead. Further, the fields CardNumber and CVV are using the type String, because they are identifiers not quantities.

Post payment endpoint: For the currency field, the error message includes all possible values. If more currency options exist a more generic message would be used.

Validation of request fields is done in the object itself (e.g. PaymentProcessingRequest), this makes sure the fields can easily be validated when used somewhere else and validation is closely connected to the data.
#### Testing
Testing at the controller level provides adequate coverage without unnecessary mock complexity. Since processPayments primarily acts as a mapper, this approach minimizes mock overhead while maintaining thorough test coverage.

Successful validation for the POST endpoint is tested in one single test. By using input data with values close to the error case most brittle cases are tested and one test is easy maintainable.
#### Service/repository layer
It is not worth using a money type for amount, because the application does not do any hard work (converting, rounding, ...)

The added logging aims to provide clear visibility into the data flow. These log entries supply sufficient context and detail to trace system activity and diagnose events effectively.

The solution includes a dedicated bank communication service to enhance testability and decouple business from integration logic.

Existing data classes like PostPaymentRequest are now of type record because it's less code and changes to these classes were needed anyway. If I work in a production code base I would stick to the type we already use.

Only payments with authorized and declined status are saved and returned. In other cases we do not know what happened to the payment, thus we need to abort the processing of it. In this case a clear error message is returned.

All existing but unused code was deleted to keep the solution as simple as possible.
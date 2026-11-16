package integration.test.fixtures.task;

import integration.test.fixtures.payload.MessageEnumTestInput;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import io.github.qishr.cascara.common.i18n.LocalizableMessage;
import io.github.qishr.cascara.common.i18n.Localizer;

public class MessageEnumTestTask extends AbstractExecutionTask<MessageEnumTestInput, String> {
    Reporter reporter = Reporter.forClass(MessageEnumTestTask.class);

    @Override
    public String run(MessageEnumTestInput input) throws Throwable {

        Localizer localizer = Localizer.instance();

        String languageTag = localizer.getActiveLocale().toLanguageTag();
        // System.out.println("Active locale: " + languageTag);

        // 1. Load the specified message enum
        Class<?> messageEnum = Class.forName(input.messageEnumFqcn);

        // System.out.println("Message enum: " + messageEnum);

        // 2. Register translations
        if (!localizer.registerTranslations(messageEnum, input.translationPath)) {
            throw new Exception("Failed to register translations");
        }

        // System.out.println("Localizer has translations? " + localizer.hasLanguage(languageTag));

        // 3. Extract the first enum constant from the loaded class
        Object[] constants = messageEnum.getEnumConstants();
        if (constants == null || constants.length == 0) {
            throw new IllegalArgumentException("Class is not an enum or has no constants: " + input);
        }

        Object enumConst = constants[0];
        // System.out.println("Enum constant: " + enumConst);

        // 4. Format localized message using Localizer
        if (enumConst instanceof LocalizableMessage message) {
            return localizer.format(message);
        }

        throw new IllegalArgumentException("Class does not implement LocalizableMessage: " + input);
    }
}

/**
 * Simple editor for IP address ranges.
 */
package de.dal33t.powerfolder.util.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTextField;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.validation.Severity;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;
import com.jgoodies.validation.Validator;
import com.jgoodies.validation.message.SimpleValidationMessage;
import com.jgoodies.validation.util.DefaultValidationResultModel;
import com.jgoodies.validation.view.ValidationResultViewFactory;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.net.AddressRange;

class AddressEditor extends BaseDialog {
    public enum EditorResult {
        OK, CANCEL
    }

    private JTextField inputLine;
    JButton okButton;
    JButton cancelButton;

    private EditorResult result;

    private ValueModel addressModel;
    private ValidationResultModel validationResultModel;

    public AddressEditor(Controller controller) {
        this(controller, "");
    }

    public AddressEditor(Controller controller, String string) {
        super(controller, true, false);
        result = EditorResult.CANCEL;
        validationResultModel = new DefaultValidationResultModel();
        
        addressModel = new ValueHolder();
        // Trigger validation when the address model changes
        addressModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                String addressText = (String) evt.getNewValue();
                ValidationResult aResult = new AddressRangeValidator(
                    addressText).validate(null);
                validationResultModel.setResult(aResult);
            }
        });
        addressModel.setValue(string);
        
        ActionListener okAction = new OKAction();
        okButton = createOKButton(okAction);
        cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        
        // Create a textfield that is bound to the address model
        inputLine = BasicComponentFactory.createTextField(addressModel, false);
        inputLine.setColumns(40);
        inputLine.addActionListener(okAction);
    }

    @Override
    protected Component getButtonBar()
    {
        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    @Override
    protected Component getContent()
    {
        FormLayout layout = new FormLayout("pref", "pref, 3dlu, 30dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        CellConstraints cc = new CellConstraints();

        builder.add(inputLine, cc.xy(1, 1));
        builder.add(ValidationResultViewFactory
            .createReportList(validationResultModel), cc.xy(1, 3));
        
        return builder.getPanel();
    }

    @Override
    protected Icon getIcon()
    {
        return null;
    }

    @Override
    public String getTitle()
    {
        return Translation.getTranslation("addressEditor.title");
    }

    private class OKAction implements ActionListener {
        public void actionPerformed(ActionEvent arg0) {
            if (!validationResultModel.getResult().hasMessages()) {
                result = EditorResult.OK;
                close();
            }
        }
    }
    
    public String getAddressRange() {
        return (String) addressModel.getValue();
    }

    public EditorResult getResult() {
        return result;
    }

    /**
     * Validates the input.
     */
    private static class AddressRangeValidator implements Validator {
        private String input;
        
        /**
         * @param input
         */
        public AddressRangeValidator(String input) {
            super();
            this.input = input;
        }

        public ValidationResult validate(Object input) {
            ValidationResult result = new ValidationResult();
            if (!isValidRange()) {
                result
                    .add(new SimpleValidationMessage(Translation
                        .getTranslation("addressEditor.parseerror"),
                        Severity.WARNING));
            }

            return result;
        }

        private boolean isValidRange() {
            try {
                return AddressRange.parseRange(input) != null;
            } catch (ParseException e) {
            }
            return false;
        }
    }
}
package org.apache.dolphinscheduler.spi.params;

import org.apache.dolphinscheduler.spi.params.base.FormType;
import org.apache.dolphinscheduler.spi.params.base.ParamsProps;
import org.apache.dolphinscheduler.spi.params.base.PluginParams;
import org.apache.dolphinscheduler.spi.params.base.PropsType;

/**
 *Text param
 */
public class PasswordParam extends PluginParams {

    public PasswordParam(String name, String label) {
        super(name, FormType.INPUT, label);
        ParamsProps paramsProps = new ParamsProps();
        paramsProps.setPropsType(PropsType.PASSWORD);
        this.setProps(paramsProps);
    }

    public PasswordParam setPlaceholder(String placeholder) {
        if(this.getProps() == null) {
            this.setProps(new ParamsProps());
        }

        this.getProps().setPlaceholder(placeholder);
        return this;
    }
}
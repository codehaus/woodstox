package wstxtest.cfg;

import com.ctc.wstx.stax.WstxInputFactory;

public interface InputTestConfig
{
    public boolean nextConfig(WstxInputFactory f);

    /**
     * Method that will reset iteration state to the initial, ie. state
     * before any iteration
     */
    public void firstConfig(WstxInputFactory f);

    /**
     * @return String that describes current settings this configuration
     *   Object has (has set when {@link #nextConfig} was called)
     */
    public String getDesc();
}

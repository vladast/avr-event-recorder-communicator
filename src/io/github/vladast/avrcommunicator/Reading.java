package io.github.vladast.avrcommunicator;

import io.github.vladast.avrcommunicator.Reading;

public class Reading {
    
    private byte     m_Entry;        // entry count
    private byte     m_Code;         // event code
    private int    m_Timestamp;    // timestamp

    public Reading()
    {
        m_Entry = 0;
        m_Code = 0;
        m_Timestamp = 0;
    }

    public Reading(Reading reading) 
    {
        m_Entry = reading.getEntry();
        m_Code = reading.getCode();
        m_Timestamp = reading.getTimestamp();
    }

    public void setEntry(final byte entryCount) 
    { 
        m_Entry = entryCount; 
    }
    
    public final byte getEntry() 
    { 
        return m_Entry; 
    }

    public void setCode(final byte code) 
    { 
        m_Code = code; 
    }
    
    public final byte getCode() 
    { 
        return m_Code; 
    }

    public void setTimestamp(final int timestamp) 
    { 
        m_Timestamp = timestamp; 
    }
    
    public final int getTimestamp() 
    { 
        return m_Timestamp; 
    }

    public final String getCodeName() {
        
        String strCodeName = "";
        
        switch(m_Code)
        {
            case 0x01:
                strCodeName = "SW01";
                break;
            case 0x02:
                strCodeName = "SW02";
                break;
            case 0x03:
                strCodeName = "SW03";
                break;
            default:
                strCodeName = "ERR [" + String.format("0x%02x", m_Code) + "]";
        }
        
        return strCodeName;
    }
}


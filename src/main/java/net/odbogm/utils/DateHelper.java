/*
 * Copyright (c) 2006 Sun Microsystems, Inc.  All rights reserved.  U.S.
 * Government Rights - Commercial software.  Government users are subject
 * to the Sun Microsystems, Inc. standard license agreement and
 * applicable provisions of the FAR and its supplements.  Use is subject
 * to license terms.
 *
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and J2EE are trademarks
 * or registered trademarks of Sun Microsystems, Inc. in the U.S. and
 * other countries.
 *
 * Copyright (c) 2006 Sun Microsystems, Inc. Tous droits reserves.
 *
 * Droits du gouvernement americain, utilisateurs gouvernementaux - logiciel
 * commercial. Les utilisateurs gouvernementaux sont soumis au contrat de
 * licence standard de Sun Microsystems, Inc., ainsi qu'aux dispositions
 * en vigueur de la FAR (Federal Acquisition Regulations) et des
 * supplements a celles-ci.  Distribue par des licences qui en
 * restreignent l'utilisation.
 *
 * Cette distribution peut comprendre des composants developpes par des
 * tierces parties. Sun, Sun Microsystems, le logo Sun, Java et J2EE
 * sont des marques de fabrique ou des marques deposees de Sun
 * Microsystems, Inc. aux Etats-Unis et dans d'autres pays.
 */


package net.odbogm.utils;

import java.util.*;
import java.text.SimpleDateFormat;


/**
 * This class contains helper methods for dealing with
 * Date objects.
 */
public final class DateHelper {
    public static final Date getDate(
        int year,
        int month,
        int day,
        int hour,
        int minute) {
        // returns a Date with the specified time elements
        Calendar cal = new GregorianCalendar(
                    year,
                    intToCalendarMonth(month),
                    day,
                    hour,
                    minute);

        return cal.getTime();
    } // getDate
    
    public static final Date getDate(
        int year,
        int month,
        int day,
        int hour,
        int minute,
        int second) {
        // returns a Date with the specified time elements
        Calendar cal = new GregorianCalendar(
                    year,
                    intToCalendarMonth(month),
                    day,
                    hour,
                    minute,
                    second);

        return cal.getTime();
    } // getDate

    public static final Date getDate(
        int year,
        int month,
        int day) {
        // returns a Date with the specified time elements,
        // with the hour and minutes both set to 0 (midnight)
        Calendar cal = new GregorianCalendar(
                    year,
                    intToCalendarMonth(month),
                    day);

        return cal.getTime();
    } // getDate

    public static final Date addDays(
        Date target,
        int days) {
        // returns a Date that is the sum of the target Date
        // and the specified number of days;
        // to subtract days from the target Date, the days
        // argument should be negative
        long msPerDay = 1000 * 60 * 60 * 24;
        long msTarget = target.getTime();
        long msSum = msTarget + (msPerDay * days);
        Date result = new Date();
        result.setTime(msSum);

        return result;
    } // addDays

    public static int dayDiff(
        Date first,
        Date second) {
        // returns the difference, in days, between the first
        // and second Date arguments
        long msPerDay = 1000 * 60 * 60 * 24;
        long diff = (first.getTime() / msPerDay)
            - (second.getTime() / msPerDay);
        Long convertLong = new Long(diff);

        return convertLong.intValue();
    } // dayDiff

    public static int getYear(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);

        return cal.get(Calendar.YEAR);
    } // getYear

    public static int getMonth(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);

        int calendarMonth = cal.get(Calendar.MONTH);

        return calendarMonthToInt(calendarMonth);
    } // getMonth

    /** 
     * retorna el día de la semana (1-7) 
     * @param date fecha de referencia
     * @return el día correspondiente
     */
    public static int getDOW(java.util.Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);

        return cal.get(Calendar.DAY_OF_WEEK);
    } // getDOW

    public static int getDay(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);

        return cal.get(Calendar.DAY_OF_MONTH);
    } // getDay

    public static int getHour(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);

        return cal.get(Calendar.HOUR_OF_DAY);
    } // getHour

    public static int getMinute(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);

        return cal.get(Calendar.MINUTE);
    } // getMinute

    public static int getSecond(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);

        return cal.get(Calendar.SECOND);
    } // getMinute

    private static int calendarMonthToInt(int calendarMonth) {
        if (calendarMonth == Calendar.JANUARY) {
            return 1;
        } else if (calendarMonth == Calendar.FEBRUARY) {
            return 2;
        } else if (calendarMonth == Calendar.MARCH) {
            return 3;
        } else if (calendarMonth == Calendar.APRIL) {
            return 4;
        } else if (calendarMonth == Calendar.MAY) {
            return 5;
        } else if (calendarMonth == Calendar.JUNE) {
            return 6;
        } else if (calendarMonth == Calendar.JULY) {
            return 7;
        } else if (calendarMonth == Calendar.AUGUST) {
            return 8;
        } else if (calendarMonth == Calendar.SEPTEMBER) {
            return 9;
        } else if (calendarMonth == Calendar.OCTOBER) {
            return 10;
        } else if (calendarMonth == Calendar.NOVEMBER) {
            return 11;
        } else if (calendarMonth == Calendar.DECEMBER) {
            return 12;
        } else {
            return 1;
        }
    } // calendarMonthToInt

    /**
     * Retorna un string con la representación de la fecha de acuerdo al formato pasado.
     * Se utiliza SimpleDateFormat para relaizar la conversió.
     * @param date feca de refenrecia
     * @param pattern: ej: "YYYY-MM-dd"
     * @return el formato correspondiente.
     */
    public static String format(
        Date date,
        String pattern) {
        // returns a String representation of the date argument,
        // formatted according to the pattern argument, which
        // has the same syntax as the argument of the SimpleDateFormat
        // class
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);

        return formatter.format(date);
    } // format

    public static String format(
        Calendar cal,
        String pattern) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);

        return formatter.format(calendarToDate(cal));
    } // format

    private static int intToCalendarMonth(int month) {
        if (month == 1) {
            return Calendar.JANUARY;
        } else if (month == 2) {
            return Calendar.FEBRUARY;
        } else if (month == 3) {
            return Calendar.MARCH;
        } else if (month == 4) {
            return Calendar.APRIL;
        } else if (month == 5) {
            return Calendar.MAY;
        } else if (month == 6) {
            return Calendar.JUNE;
        } else if (month == 7) {
            return Calendar.JULY;
        } else if (month == 8) {
            return Calendar.AUGUST;
        } else if (month == 9) {
            return Calendar.SEPTEMBER;
        } else if (month == 10) {
            return Calendar.OCTOBER;
        } else if (month == 11) {
            return Calendar.NOVEMBER;
        } else if (month == 12) {
            return Calendar.DECEMBER;
        } else {
            return Calendar.JANUARY;
        }
    } // intToCalendarMonth

    public static Calendar dateToCalendar(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);

        return cal;
    } // dateToCalendar

    public static Date calendarToDate(Calendar cal) {
        if (cal == null) {
            return null;
        }
        return cal.getTime();
    } // calendarToDate

    public static java.sql.Date calendarToSqlDate(Calendar cal) {
        if (cal == null) {
            return null;
        }
        return new java.sql.Date(cal.getTime().getTime());
    } // calendarToSqlDate
    
    public static java.sql.Date dateToSqlDate(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return new java.sql.Date(date.getTime());
    } // dateToSqlDate

    public static java.util.Date sqlDateToDate(java.sql.Date date) {
        if (date == null) {
            return null;
        }
        return new java.util.Date(date.getTime());
    }

    /**
     * retorna la fecha actual con la hora puesta a 00:00
     * @return fecha actual.
     */
    public static java.util.Date getCurrentDate() {
        int yy = DateHelper.getYear(DateHelper.getCurrentDateTime());
        int mm = DateHelper.getMonth(DateHelper.getCurrentDateTime());
        int dd = DateHelper.getDay(DateHelper.getCurrentDateTime());
        
        return DateHelper.getDate(yy, mm, dd);
    }
    
    /**
     * Retorna una cadena con la fecha actual en el formato requerido.
     * Se utiliza SimpleDateFormat.
     * @param format formato a usar
     * @return fecha actual de acuerdo al formato.
     */
    public static String getCurrentDate(String format) {
        return DateHelper.format(DateHelper.getCurrentDate(), format);
    }
    
    
    /**
     * retorna la fecha y hora actual
     * @return fecha y hora actual
     */
    public static java.util.Date getCurrentDateTime() {
        return Calendar.getInstance().getTime();
    }

    public static java.sql.Date getCurrentSqlDate() {
        return new java.sql.Date(Calendar.getInstance().getTime().getTime());
    }

    public static final java.sql.Date getSqlDate(
        int year,
        int month,
        int day) {
        // returns a Date with the specified time elements,
        // with the hour and minutes both set to 0 (midnight)
        Calendar cal = new GregorianCalendar(
                    year,
                    intToCalendarMonth(month),
                    day);

        return new java.sql.Date(cal.getTime().getTime());
    } // getDate

    /**
     * Crea una instancia de Calendar en base a un java.sql.Date
     * @param fecha la fecha a convertir
     * @return Calendar inicializado con la fecha de referencia.
     */
    public static Calendar sqlDateToCalendar(java.sql.Date fecha) {
        if (fecha == null) {
            return null;
        }
        Calendar result = Calendar.getInstance();
        result.setTime(new java.util.Date(fecha.getTime()));
        return result;
    } // dateToCalendar

    public static int getSqlYear(java.sql.Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);

        return cal.get(Calendar.YEAR);
    } // getSqlYear

    public static int getSqlMonth(java.sql.Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);

        int calendarMonth = cal.get(Calendar.MONTH);

        return calendarMonthToInt(calendarMonth);
    } // getSqlMonth

    public static int getSqlDay(java.sql.Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);

        return cal.get(Calendar.DAY_OF_MONTH);
    } // getSqlDay

    public static String calendarToString(Calendar cal) {
        String resu = "";
        
        int dia = cal.get(Calendar.DATE);
        int mes = cal.get(Calendar.MONTH)+1;
        int anio = cal.get(Calendar.YEAR);
        
        resu = (dia < 10 ? "0" : "") + dia + "/" +
                (mes < 10 ? "0" : "") + mes + "/" +
                anio;
        return resu;
    }

    public static String getPeriodo(Calendar periodo) {
        Date date = calendarToDate(periodo);
        return getPeriodo(date);
    }
    
    public static String getPeriodo(java.sql.Date periodo) {
        Date date = sqlDateToDate(periodo);
        return getPeriodo(date);
    }

    public static String getPeriodo(Date periodo) {
        int month = getMonth(periodo);
        int year = getYear(periodo);
        
        String sPeriodo = (month < 10 ? "0" : "") + month + "/" + year;

        return sPeriodo;
    }

    /**
     * retorna la fecha en formato string eliminando todos los caracteres separadores
     * quedando con la siguiente cadena: yyyymmddhhmmss
     *
     * @param date fecha de referencia
     * @return String con el formato yyyymmddhhmmss
     */
    public static String dtos(Date date){
        int yyyy = getYear(date);
        int mm = getMonth(date);
        int dd = getDay(date);
        int hh = getHour(date);
        int mi = getMinute(date);
        int ss = getSecond(date);

        return yyyy<100?("20"+yyyy):(""+yyyy) +
               (mm<10?("0"+mm):(""+mm)) +
               (dd<10?("0"+dd):(""+dd)) +
               (hh<10?("0"+hh):(""+hh)) +
               (mi<10?("0"+mi):(""+mi)) +
               (ss<10?("0"+ss):(""+ss)) ;
    }

    /**
     * Retorna una cadena en formato dd/MM/yyyy
     * @param date fecha de referencia.
     * @return String con el formato dd/MM/yyyy
     */
    public static String toScreenableString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat ("dd/MM/yyyy");
        String texto = sdf.format( date );
        return texto;
    }
} 

// class

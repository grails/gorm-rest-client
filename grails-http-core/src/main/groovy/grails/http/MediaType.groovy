package grails.http

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode

/**
 * Represents a media type
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
@EqualsAndHashCode(includes = ['name'])
class MediaType implements CharSequence {

    public static final MediaType ALL = new MediaType("*/*", "all")
    public static final MediaType FORM = new MediaType("application/x-www-form-urlencoded", "form")
    public static final MediaType MULTIPART_FORM = new MediaType("multipart/form-data", "multipartForm")
    public static final MediaType HTML = new MediaType('text/html')
    public static final MediaType XHTML = new MediaType("application/xhtml+xml", "html")
    public static final MediaType XML = new MediaType('application/xml')
    public static final MediaType JSON = new MediaType('application/json')
    public static final MediaType TEXT_XML = new MediaType('text/xml')
    public static final MediaType TEXT_JSON = new MediaType('text/json')
    public static final MediaType HAL_JSON = new MediaType('application/hal+json')
    public static final MediaType HAL_XML = new MediaType('application/hal+xml')
    public static final MediaType ATOM_XML = new MediaType('application/atom+xml')
    public static final MediaType VND_ERROR = new MediaType("application/vnd.error+json")

    public static final String QUALITY_RATING = "1.0"
    public static final BigDecimal QUALITY_RATING_NUMBER = 1.0

    protected final String name
    protected final String subtype
    protected final String type
    protected final String fullName
    protected final String extension
    protected final Map<String, String> parameters = [q: QUALITY_RATING]

    private BigDecimal qualityNumberField

    MediaType(String name, Map params = [:]) {
        this(name, null, params)
    }

    MediaType(String name, String extension, Map<String, String> params = [:]) {
        this.fullName = name
        if(name && name.contains(';')) {
            String[] tokenWithArgs = name.split(';')
            name = tokenWithArgs[0]
            List<String> paramsList = tokenWithArgs[1..-1]
            for(String param in paramsList) {
                def i = param.indexOf('=')
                if (i > -1) {
                    parameters[param[0..i-1].trim()] = param[i+1..-1].trim()
                }
            }
        }
        this.name = name
        int i = name.indexOf('/')
        if(i > -1) {
            this.type = name.substring(0, i)
            this.subtype = name.substring(i + 1, name.length())
        }
        else {
            throw new IllegalArgumentException("Invalid mime type $name")
        }

        if(extension != null) {
            this.extension = extension
        }
        else {
            int j = subtype.indexOf('+')
            if(j > -1) {
                this.extension = subtype.substring(j + 1)
            }
            else {
                this.extension = subtype
            }
        }
        parameters.putAll(params)
    }

    /**
     * @return Full name with parameters
     */
    String getFullName() {
        return fullName
    }
    /**
     * @return The name of the mime type without any parameters
     */
    String getName() {
        return name
    }

    /**
     * @return The type of the media type. For example for application/hal+json this would return "application"
     */
    String getType() {
        return this.type
    }

    /**
     * @return The subtype. For example for application/hal+json this would return "hal+json"
     */
    String getSubtype() {
        return this.subtype
    }

    /**
     * @return The extension. For example for application/hal+json this would return "json"
     */
    String getExtension() {
        return extension
    }

    /**
     * @return The parameters to the media type
     */
    Map<String, String> getParameters() {
        return parameters
    }

    /**
     * @return The quality of the Mime type
     */
    String getQuality() {
        return parameters.q ?: QUALITY_RATING
    }

    /**
     * @return The quality in BigDecimal form
     */
    BigDecimal getQualityAsNumber() {
        if(this.qualityNumberField == null) {
            this.qualityNumberField = getOrConvertQualityParameterToBigDecimal(this)
        }
        return this.qualityNumberField
    }

    /**
     * @return The version of the Mime type
     */
    String getVersion() {
        return parameters.v ?: null
    }


    @Override
    int length() {
        return toString().length()
    }

    @Override
    char charAt(int index) {
        return toString().charAt(index)
    }

    @Override
    CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end)
    }

    String toString() {
        return fullName
    }

    private BigDecimal getOrConvertQualityParameterToBigDecimal(MediaType mt) {
        BigDecimal bd
        try {
            def q = mt.parameters.q
            if(q == null) return QUALITY_RATING_NUMBER
            else {
                bd = q.toString().toBigDecimal()
                // replace to avoid expensive conversion again
                mt.parameters.q = bd
            }
            return bd
        } catch (NumberFormatException e) {
            bd = QUALITY_RATING_NUMBER
            return bd
        }
    }
}

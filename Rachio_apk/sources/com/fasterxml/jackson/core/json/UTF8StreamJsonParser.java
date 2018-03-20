package com.fasterxml.jackson.core.json;

import android.support.v7.widget.RecyclerView.ItemAnimator;
import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.shinobicontrols.charts.R;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;

public class UTF8StreamJsonParser extends ParserBase {
    protected static final int[] _icLatin1 = CharTypes.getInputCodeLatin1();
    private static final int[] _icUTF8 = CharTypes.getInputCodeUtf8();
    protected boolean _bufferRecyclable;
    protected byte[] _inputBuffer;
    protected InputStream _inputStream;
    protected int _nameStartCol;
    protected int _nameStartOffset;
    protected int _nameStartRow;
    protected ObjectCodec _objectCodec;
    private int _quad1;
    protected int[] _quadBuffer = new int[16];
    protected final ByteQuadsCanonicalizer _symbols;
    protected boolean _tokenIncomplete;

    public UTF8StreamJsonParser(IOContext ctxt, int features, InputStream in, ObjectCodec codec, ByteQuadsCanonicalizer sym, byte[] inputBuffer, int start, int end, boolean bufferRecyclable) {
        super(ctxt, features);
        this._inputStream = in;
        this._objectCodec = codec;
        this._symbols = sym;
        this._inputBuffer = inputBuffer;
        this._inputPtr = start;
        this._inputEnd = end;
        this._currInputRowStart = start;
        this._currInputProcessed = (long) (-start);
        this._bufferRecyclable = bufferRecyclable;
    }

    public ObjectCodec getCodec() {
        return this._objectCodec;
    }

    public void setCodec(ObjectCodec c) {
        this._objectCodec = c;
    }

    public int releaseBuffered(OutputStream out) throws IOException {
        int count = this._inputEnd - this._inputPtr;
        if (count <= 0) {
            return 0;
        }
        out.write(this._inputBuffer, this._inputPtr, count);
        return count;
    }

    public Object getInputSource() {
        return this._inputStream;
    }

    protected final boolean _loadMore() throws IOException {
        int bufSize = this._inputEnd;
        this._currInputProcessed += (long) this._inputEnd;
        this._currInputRowStart -= this._inputEnd;
        this._nameStartOffset -= bufSize;
        if (this._inputStream == null) {
            return false;
        }
        int space = this._inputBuffer.length;
        if (space == 0) {
            return false;
        }
        int count = this._inputStream.read(this._inputBuffer, 0, space);
        if (count > 0) {
            this._inputPtr = 0;
            this._inputEnd = count;
            return true;
        }
        _closeInput();
        if (count != 0) {
            return false;
        }
        throw new IOException("InputStream.read() returned 0 characters when trying to read " + this._inputBuffer.length + " bytes");
    }

    protected void _closeInput() throws IOException {
        if (this._inputStream != null) {
            if (this._ioContext.isResourceManaged() || isEnabled(Feature.AUTO_CLOSE_SOURCE)) {
                this._inputStream.close();
            }
            this._inputStream = null;
        }
    }

    protected void _releaseBuffers() throws IOException {
        super._releaseBuffers();
        this._symbols.release();
        if (this._bufferRecyclable) {
            byte[] buf = this._inputBuffer;
            if (buf != null) {
                this._inputBuffer = ByteArrayBuilder.NO_BYTES;
                this._ioContext.releaseReadIOBuffer(buf);
            }
        }
    }

    public String getText() throws IOException {
        if (this._currToken != JsonToken.VALUE_STRING) {
            return _getText2(this._currToken);
        }
        if (!this._tokenIncomplete) {
            return this._textBuffer.contentsAsString();
        }
        this._tokenIncomplete = false;
        return _finishAndReturnString();
    }

    public int getText(Writer writer) throws IOException {
        JsonToken t = this._currToken;
        if (t == JsonToken.VALUE_STRING) {
            if (this._tokenIncomplete) {
                this._tokenIncomplete = false;
                _finishString();
            }
            return this._textBuffer.contentsToWriter(writer);
        } else if (t == JsonToken.FIELD_NAME) {
            String n = this._parsingContext.getCurrentName();
            writer.write(n);
            return n.length();
        } else if (t == null) {
            return 0;
        } else {
            if (t.isNumeric()) {
                return this._textBuffer.contentsToWriter(writer);
            }
            char[] ch = t.asCharArray();
            writer.write(ch);
            return ch.length;
        }
    }

    public String getValueAsString() throws IOException {
        if (this._currToken == JsonToken.VALUE_STRING) {
            if (!this._tokenIncomplete) {
                return this._textBuffer.contentsAsString();
            }
            this._tokenIncomplete = false;
            return _finishAndReturnString();
        } else if (this._currToken == JsonToken.FIELD_NAME) {
            return getCurrentName();
        } else {
            return super.getValueAsString(null);
        }
    }

    public String getValueAsString(String defValue) throws IOException {
        if (this._currToken == JsonToken.VALUE_STRING) {
            if (!this._tokenIncomplete) {
                return this._textBuffer.contentsAsString();
            }
            this._tokenIncomplete = false;
            return _finishAndReturnString();
        } else if (this._currToken == JsonToken.FIELD_NAME) {
            return getCurrentName();
        } else {
            return super.getValueAsString(defValue);
        }
    }

    public int getValueAsInt() throws IOException {
        JsonToken t = this._currToken;
        if (t != JsonToken.VALUE_NUMBER_INT && t != JsonToken.VALUE_NUMBER_FLOAT) {
            return super.getValueAsInt(0);
        }
        if ((this._numTypesValid & 1) == 0) {
            if (this._numTypesValid == 0) {
                return _parseIntValue();
            }
            if ((this._numTypesValid & 1) == 0) {
                convertNumberToInt();
            }
        }
        return this._numberInt;
    }

    public int getValueAsInt(int defValue) throws IOException {
        JsonToken t = this._currToken;
        if (t != JsonToken.VALUE_NUMBER_INT && t != JsonToken.VALUE_NUMBER_FLOAT) {
            return super.getValueAsInt(defValue);
        }
        if ((this._numTypesValid & 1) == 0) {
            if (this._numTypesValid == 0) {
                return _parseIntValue();
            }
            if ((this._numTypesValid & 1) == 0) {
                convertNumberToInt();
            }
        }
        return this._numberInt;
    }

    protected final String _getText2(JsonToken t) {
        if (t == null) {
            return null;
        }
        switch (t.id()) {
            case 5:
                return this._parsingContext.getCurrentName();
            case 6:
            case 7:
            case 8:
                return this._textBuffer.contentsAsString();
            default:
                return t.asString();
        }
    }

    public char[] getTextCharacters() throws IOException {
        if (this._currToken == null) {
            return null;
        }
        switch (this._currToken.id()) {
            case 5:
                if (!this._nameCopied) {
                    String name = this._parsingContext.getCurrentName();
                    int nameLen = name.length();
                    if (this._nameCopyBuffer == null) {
                        this._nameCopyBuffer = this._ioContext.allocNameCopyBuffer(nameLen);
                    } else if (this._nameCopyBuffer.length < nameLen) {
                        this._nameCopyBuffer = new char[nameLen];
                    }
                    name.getChars(0, nameLen, this._nameCopyBuffer, 0);
                    this._nameCopied = true;
                }
                return this._nameCopyBuffer;
            case 6:
                if (this._tokenIncomplete) {
                    this._tokenIncomplete = false;
                    _finishString();
                    break;
                }
                break;
            case 7:
            case 8:
                break;
            default:
                return this._currToken.asCharArray();
        }
        return this._textBuffer.getTextBuffer();
    }

    public int getTextLength() throws IOException {
        if (this._currToken == null) {
            return 0;
        }
        switch (this._currToken.id()) {
            case 5:
                return this._parsingContext.getCurrentName().length();
            case 6:
                if (this._tokenIncomplete) {
                    this._tokenIncomplete = false;
                    _finishString();
                    break;
                }
                break;
            case 7:
            case 8:
                break;
            default:
                return this._currToken.asCharArray().length;
        }
        return this._textBuffer.size();
    }

    public int getTextOffset() throws IOException {
        if (this._currToken == null) {
            return 0;
        }
        switch (this._currToken.id()) {
            case 6:
                if (this._tokenIncomplete) {
                    this._tokenIncomplete = false;
                    _finishString();
                    break;
                }
                break;
            case 7:
            case 8:
                break;
            default:
                return 0;
        }
        return this._textBuffer.getTextOffset();
    }

    public byte[] getBinaryValue(Base64Variant b64variant) throws IOException {
        if (this._currToken != JsonToken.VALUE_STRING && (this._currToken != JsonToken.VALUE_EMBEDDED_OBJECT || this._binaryValue == null)) {
            _reportError("Current token (" + this._currToken + ") not VALUE_STRING or VALUE_EMBEDDED_OBJECT, can not access as binary");
        }
        if (this._tokenIncomplete) {
            try {
                this._binaryValue = _decodeBase64(b64variant);
                this._tokenIncomplete = false;
            } catch (IllegalArgumentException iae) {
                throw _constructError("Failed to decode VALUE_STRING as base64 (" + b64variant + "): " + iae.getMessage());
            }
        } else if (this._binaryValue == null) {
            ByteArrayBuilder builder = _getByteArrayBuilder();
            _decodeBase64(getText(), builder, b64variant);
            this._binaryValue = builder.toByteArray();
        }
        return this._binaryValue;
    }

    public int readBinaryValue(Base64Variant b64variant, OutputStream out) throws IOException {
        if (this._tokenIncomplete && this._currToken == JsonToken.VALUE_STRING) {
            byte[] buf = this._ioContext.allocBase64Buffer();
            try {
                int _readBinary = _readBinary(b64variant, out, buf);
                return _readBinary;
            } finally {
                this._ioContext.releaseBase64Buffer(buf);
            }
        } else {
            byte[] b = getBinaryValue(b64variant);
            out.write(b);
            return b.length;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected int _readBinary(com.fasterxml.jackson.core.Base64Variant r11, java.io.OutputStream r12, byte[] r13) throws java.io.IOException {
        /*
        r10 = this;
        r5 = 0;
        r7 = r13.length;
        r4 = r7 + -3;
        r3 = 0;
    L_0x0005:
        r7 = r10._inputPtr;
        r8 = r10._inputEnd;
        if (r7 < r8) goto L_0x000e;
    L_0x000b:
        r10._loadMoreGuaranteed();
    L_0x000e:
        r7 = r10._inputBuffer;
        r8 = r10._inputPtr;
        r9 = r8 + 1;
        r10._inputPtr = r9;
        r7 = r7[r8];
        r1 = r7 & 255;
        r7 = 32;
        if (r1 <= r7) goto L_0x0005;
    L_0x001e:
        r0 = r11.decodeBase64Char(r1);
        if (r0 >= 0) goto L_0x002f;
    L_0x0024:
        r7 = 34;
        if (r1 == r7) goto L_0x008c;
    L_0x0028:
        r7 = 0;
        r0 = r10._decodeBase64Escape(r11, r1, r7);
        if (r0 < 0) goto L_0x0005;
    L_0x002f:
        if (r5 <= r4) goto L_0x0037;
    L_0x0031:
        r3 = r3 + r5;
        r7 = 0;
        r12.write(r13, r7, r5);
        r5 = 0;
    L_0x0037:
        r2 = r0;
        r7 = r10._inputPtr;
        r8 = r10._inputEnd;
        if (r7 < r8) goto L_0x0041;
    L_0x003e:
        r10._loadMoreGuaranteed();
    L_0x0041:
        r7 = r10._inputBuffer;
        r8 = r10._inputPtr;
        r9 = r8 + 1;
        r10._inputPtr = r9;
        r7 = r7[r8];
        r1 = r7 & 255;
        r0 = r11.decodeBase64Char(r1);
        if (r0 >= 0) goto L_0x0058;
    L_0x0053:
        r7 = 1;
        r0 = r10._decodeBase64Escape(r11, r1, r7);
    L_0x0058:
        r7 = r2 << 6;
        r2 = r7 | r0;
        r7 = r10._inputPtr;
        r8 = r10._inputEnd;
        if (r7 < r8) goto L_0x0065;
    L_0x0062:
        r10._loadMoreGuaranteed();
    L_0x0065:
        r7 = r10._inputBuffer;
        r8 = r10._inputPtr;
        r9 = r8 + 1;
        r10._inputPtr = r9;
        r7 = r7[r8];
        r1 = r7 & 255;
        r0 = r11.decodeBase64Char(r1);
        if (r0 >= 0) goto L_0x00e3;
    L_0x0077:
        r7 = -2;
        if (r0 == r7) goto L_0x009c;
    L_0x007a:
        r7 = 34;
        if (r1 != r7) goto L_0x0097;
    L_0x007e:
        r7 = r11.usesPadding();
        if (r7 != 0) goto L_0x0097;
    L_0x0084:
        r2 = r2 >> 4;
        r6 = r5 + 1;
        r7 = (byte) r2;
        r13[r5] = r7;
        r5 = r6;
    L_0x008c:
        r7 = 0;
        r10._tokenIncomplete = r7;
        if (r5 <= 0) goto L_0x0096;
    L_0x0091:
        r3 = r3 + r5;
        r7 = 0;
        r12.write(r13, r7, r5);
    L_0x0096:
        return r3;
    L_0x0097:
        r7 = 2;
        r0 = r10._decodeBase64Escape(r11, r1, r7);
    L_0x009c:
        r7 = -2;
        if (r0 != r7) goto L_0x00e3;
    L_0x009f:
        r7 = r10._inputPtr;
        r8 = r10._inputEnd;
        if (r7 < r8) goto L_0x00a8;
    L_0x00a5:
        r10._loadMoreGuaranteed();
    L_0x00a8:
        r7 = r10._inputBuffer;
        r8 = r10._inputPtr;
        r9 = r8 + 1;
        r10._inputPtr = r9;
        r7 = r7[r8];
        r1 = r7 & 255;
        r7 = r11.usesPaddingChar(r1);
        if (r7 != 0) goto L_0x00d9;
    L_0x00ba:
        r7 = 3;
        r8 = new java.lang.StringBuilder;
        r9 = "expected padding character '";
        r8.<init>(r9);
        r9 = r11.getPaddingChar();
        r8 = r8.append(r9);
        r9 = "'";
        r8 = r8.append(r9);
        r8 = r8.toString();
        r7 = r10.reportInvalidBase64Char(r11, r1, r7, r8);
        throw r7;
    L_0x00d9:
        r2 = r2 >> 4;
        r6 = r5 + 1;
        r7 = (byte) r2;
        r13[r5] = r7;
        r5 = r6;
        goto L_0x0005;
    L_0x00e3:
        r7 = r2 << 6;
        r2 = r7 | r0;
        r7 = r10._inputPtr;
        r8 = r10._inputEnd;
        if (r7 < r8) goto L_0x00f0;
    L_0x00ed:
        r10._loadMoreGuaranteed();
    L_0x00f0:
        r7 = r10._inputBuffer;
        r8 = r10._inputPtr;
        r9 = r8 + 1;
        r10._inputPtr = r9;
        r7 = r7[r8];
        r1 = r7 & 255;
        r0 = r11.decodeBase64Char(r1);
        if (r0 >= 0) goto L_0x0137;
    L_0x0102:
        r7 = -2;
        if (r0 == r7) goto L_0x0124;
    L_0x0105:
        r7 = 34;
        if (r1 != r7) goto L_0x011f;
    L_0x0109:
        r7 = r11.usesPadding();
        if (r7 != 0) goto L_0x011f;
    L_0x010f:
        r2 = r2 >> 2;
        r6 = r5 + 1;
        r7 = r2 >> 8;
        r7 = (byte) r7;
        r13[r5] = r7;
        r5 = r6 + 1;
        r7 = (byte) r2;
        r13[r6] = r7;
        goto L_0x008c;
    L_0x011f:
        r7 = 3;
        r0 = r10._decodeBase64Escape(r11, r1, r7);
    L_0x0124:
        r7 = -2;
        if (r0 != r7) goto L_0x0137;
    L_0x0127:
        r2 = r2 >> 2;
        r6 = r5 + 1;
        r7 = r2 >> 8;
        r7 = (byte) r7;
        r13[r5] = r7;
        r5 = r6 + 1;
        r7 = (byte) r2;
        r13[r6] = r7;
        goto L_0x0005;
    L_0x0137:
        r7 = r2 << 6;
        r2 = r7 | r0;
        r6 = r5 + 1;
        r7 = r2 >> 16;
        r7 = (byte) r7;
        r13[r5] = r7;
        r5 = r6 + 1;
        r7 = r2 >> 8;
        r7 = (byte) r7;
        r13[r6] = r7;
        r6 = r5 + 1;
        r7 = (byte) r2;
        r13[r5] = r7;
        r5 = r6;
        goto L_0x0005;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.fasterxml.jackson.core.json.UTF8StreamJsonParser._readBinary(com.fasterxml.jackson.core.Base64Variant, java.io.OutputStream, byte[]):int");
    }

    public JsonToken nextToken() throws IOException {
        if (this._currToken == JsonToken.FIELD_NAME) {
            return _nextAfterName();
        }
        this._numTypesValid = 0;
        if (this._tokenIncomplete) {
            _skipString();
        }
        int i = _skipWSOrEnd();
        if (i < 0) {
            close();
            this._currToken = null;
            return null;
        }
        this._binaryValue = null;
        JsonToken jsonToken;
        if (i == 93) {
            _updateLocation();
            if (!this._parsingContext.inArray()) {
                _reportMismatchedEndMarker(i, '}');
            }
            this._parsingContext = this._parsingContext.clearAndGetParent();
            jsonToken = JsonToken.END_ARRAY;
            this._currToken = jsonToken;
            return jsonToken;
        } else if (i == 125) {
            _updateLocation();
            if (!this._parsingContext.inObject()) {
                _reportMismatchedEndMarker(i, ']');
            }
            this._parsingContext = this._parsingContext.clearAndGetParent();
            jsonToken = JsonToken.END_OBJECT;
            this._currToken = jsonToken;
            return jsonToken;
        } else {
            if (this._parsingContext.expectComma()) {
                if (i != 44) {
                    _reportUnexpectedChar(i, "was expecting comma to separate " + this._parsingContext.typeDesc() + " entries");
                }
                i = _skipWS();
            }
            if (this._parsingContext.inObject()) {
                _updateNameLocation();
                this._parsingContext.setCurrentName(_parseName(i));
                this._currToken = JsonToken.FIELD_NAME;
                i = _skipColon();
                _updateLocation();
                if (i == 34) {
                    this._tokenIncomplete = true;
                    this._nextToken = JsonToken.VALUE_STRING;
                    return this._currToken;
                }
                JsonToken t;
                switch (i) {
                    case R.styleable.ChartTheme_sc_seriesLineColor4 /*45*/:
                        t = _parseNegNumber();
                        break;
                    case R.styleable.ChartTheme_sc_seriesAreaGradientTransparentColor4 /*48*/:
                    case R.styleable.ChartTheme_sc_seriesLineColor5 /*49*/:
                    case 50:
                    case R.styleable.ChartTheme_sc_seriesAreaGradientColor5 /*51*/:
                    case R.styleable.ChartTheme_sc_seriesAreaGradientTransparentColor5 /*52*/:
                    case R.styleable.ChartTheme_sc_seriesLineColor6 /*53*/:
                    case R.styleable.ChartTheme_sc_seriesAreaColor6 /*54*/:
                    case R.styleable.ChartTheme_sc_seriesAreaGradientColor6 /*55*/:
                    case R.styleable.ChartTheme_sc_seriesAreaGradientTransparentColor6 /*56*/:
                    case R.styleable.ChartTheme_sc_pieDonutFlavorColor1 /*57*/:
                        t = _parsePosNumber(i);
                        break;
                    case 91:
                        t = JsonToken.START_ARRAY;
                        break;
                    case 102:
                        _matchToken("false", 1);
                        t = JsonToken.VALUE_FALSE;
                        break;
                    case 110:
                        _matchToken("null", 1);
                        t = JsonToken.VALUE_NULL;
                        break;
                    case 116:
                        _matchToken("true", 1);
                        t = JsonToken.VALUE_TRUE;
                        break;
                    case 123:
                        t = JsonToken.START_OBJECT;
                        break;
                    default:
                        t = _handleUnexpectedValue(i);
                        break;
                }
                this._nextToken = t;
                return this._currToken;
            }
            _updateLocation();
            return _nextTokenNotInObject(i);
        }
    }

    private final JsonToken _nextTokenNotInObject(int i) throws IOException {
        if (i == 34) {
            this._tokenIncomplete = true;
            JsonToken jsonToken = JsonToken.VALUE_STRING;
            this._currToken = jsonToken;
            return jsonToken;
        }
        switch (i) {
            case R.styleable.ChartTheme_sc_seriesLineColor4 /*45*/:
                jsonToken = _parseNegNumber();
                this._currToken = jsonToken;
                return jsonToken;
            case R.styleable.ChartTheme_sc_seriesAreaGradientTransparentColor4 /*48*/:
            case R.styleable.ChartTheme_sc_seriesLineColor5 /*49*/:
            case 50:
            case R.styleable.ChartTheme_sc_seriesAreaGradientColor5 /*51*/:
            case R.styleable.ChartTheme_sc_seriesAreaGradientTransparentColor5 /*52*/:
            case R.styleable.ChartTheme_sc_seriesLineColor6 /*53*/:
            case R.styleable.ChartTheme_sc_seriesAreaColor6 /*54*/:
            case R.styleable.ChartTheme_sc_seriesAreaGradientColor6 /*55*/:
            case R.styleable.ChartTheme_sc_seriesAreaGradientTransparentColor6 /*56*/:
            case R.styleable.ChartTheme_sc_pieDonutFlavorColor1 /*57*/:
                jsonToken = _parsePosNumber(i);
                this._currToken = jsonToken;
                return jsonToken;
            case 91:
                this._parsingContext = this._parsingContext.createChildArrayContext(this._tokenInputRow, this._tokenInputCol);
                jsonToken = JsonToken.START_ARRAY;
                this._currToken = jsonToken;
                return jsonToken;
            case 102:
                _matchToken("false", 1);
                jsonToken = JsonToken.VALUE_FALSE;
                this._currToken = jsonToken;
                return jsonToken;
            case 110:
                _matchToken("null", 1);
                jsonToken = JsonToken.VALUE_NULL;
                this._currToken = jsonToken;
                return jsonToken;
            case 116:
                _matchToken("true", 1);
                jsonToken = JsonToken.VALUE_TRUE;
                this._currToken = jsonToken;
                return jsonToken;
            case 123:
                this._parsingContext = this._parsingContext.createChildObjectContext(this._tokenInputRow, this._tokenInputCol);
                jsonToken = JsonToken.START_OBJECT;
                this._currToken = jsonToken;
                return jsonToken;
            default:
                jsonToken = _handleUnexpectedValue(i);
                this._currToken = jsonToken;
                return jsonToken;
        }
    }

    private final JsonToken _nextAfterName() {
        this._nameCopied = false;
        JsonToken t = this._nextToken;
        this._nextToken = null;
        if (t == JsonToken.START_ARRAY) {
            this._parsingContext = this._parsingContext.createChildArrayContext(this._tokenInputRow, this._tokenInputCol);
        } else if (t == JsonToken.START_OBJECT) {
            this._parsingContext = this._parsingContext.createChildObjectContext(this._tokenInputRow, this._tokenInputCol);
        }
        this._currToken = t;
        return t;
    }

    public void finishToken() throws IOException {
        if (this._tokenIncomplete) {
            this._tokenIncomplete = false;
            _finishString();
        }
    }

    public boolean nextFieldName(SerializableString str) throws IOException {
        this._numTypesValid = 0;
        if (this._currToken == JsonToken.FIELD_NAME) {
            _nextAfterName();
            return false;
        }
        if (this._tokenIncomplete) {
            _skipString();
        }
        int i = _skipWSOrEnd();
        if (i < 0) {
            close();
            this._currToken = null;
            return false;
        }
        this._binaryValue = null;
        if (i == 93) {
            _updateLocation();
            if (!this._parsingContext.inArray()) {
                _reportMismatchedEndMarker(i, '}');
            }
            this._parsingContext = this._parsingContext.clearAndGetParent();
            this._currToken = JsonToken.END_ARRAY;
            return false;
        } else if (i == 125) {
            _updateLocation();
            if (!this._parsingContext.inObject()) {
                _reportMismatchedEndMarker(i, ']');
            }
            this._parsingContext = this._parsingContext.clearAndGetParent();
            this._currToken = JsonToken.END_OBJECT;
            return false;
        } else {
            if (this._parsingContext.expectComma()) {
                if (i != 44) {
                    _reportUnexpectedChar(i, "was expecting comma to separate " + this._parsingContext.typeDesc() + " entries");
                }
                i = _skipWS();
            }
            if (this._parsingContext.inObject()) {
                _updateNameLocation();
                if (i == 34) {
                    byte[] nameBytes = str.asQuotedUTF8();
                    int len = nameBytes.length;
                    if ((this._inputPtr + len) + 4 < this._inputEnd) {
                        int end = this._inputPtr + len;
                        if (this._inputBuffer[end] == (byte) 34) {
                            int offset = 0;
                            int ptr = this._inputPtr;
                            while (ptr != end) {
                                if (nameBytes[offset] == this._inputBuffer[ptr]) {
                                    offset++;
                                    ptr++;
                                }
                            }
                            this._parsingContext.setCurrentName(str.getValue());
                            _isNextTokenNameYes(_skipColonFast(ptr + 1));
                            return true;
                        }
                    }
                }
                return _isNextTokenNameMaybe(i, str);
            }
            _updateLocation();
            _nextTokenNotInObject(i);
            return false;
        }
    }

    public String nextFieldName() throws IOException {
        String str = null;
        this._numTypesValid = 0;
        if (this._currToken == JsonToken.FIELD_NAME) {
            _nextAfterName();
        } else {
            if (this._tokenIncomplete) {
                _skipString();
            }
            int i = _skipWSOrEnd();
            if (i < 0) {
                close();
                this._currToken = null;
            } else {
                this._binaryValue = null;
                if (i == 93) {
                    _updateLocation();
                    if (!this._parsingContext.inArray()) {
                        _reportMismatchedEndMarker(i, '}');
                    }
                    this._parsingContext = this._parsingContext.clearAndGetParent();
                    this._currToken = JsonToken.END_ARRAY;
                } else if (i == 125) {
                    _updateLocation();
                    if (!this._parsingContext.inObject()) {
                        _reportMismatchedEndMarker(i, ']');
                    }
                    this._parsingContext = this._parsingContext.clearAndGetParent();
                    this._currToken = JsonToken.END_OBJECT;
                } else {
                    if (this._parsingContext.expectComma()) {
                        if (i != 44) {
                            _reportUnexpectedChar(i, "was expecting comma to separate " + this._parsingContext.typeDesc() + " entries");
                        }
                        i = _skipWS();
                    }
                    if (this._parsingContext.inObject()) {
                        _updateNameLocation();
                        str = _parseName(i);
                        this._parsingContext.setCurrentName(str);
                        this._currToken = JsonToken.FIELD_NAME;
                        i = _skipColon();
                        _updateLocation();
                        if (i == 34) {
                            this._tokenIncomplete = true;
                            this._nextToken = JsonToken.VALUE_STRING;
                        } else {
                            JsonToken t;
                            switch (i) {
                                case R.styleable.ChartTheme_sc_seriesLineColor4 /*45*/:
                                    t = _parseNegNumber();
                                    break;
                                case R.styleable.ChartTheme_sc_seriesAreaGradientTransparentColor4 /*48*/:
                                case R.styleable.ChartTheme_sc_seriesLineColor5 /*49*/:
                                case 50:
                                case R.styleable.ChartTheme_sc_seriesAreaGradientColor5 /*51*/:
                                case R.styleable.ChartTheme_sc_seriesAreaGradientTransparentColor5 /*52*/:
                                case R.styleable.ChartTheme_sc_seriesLineColor6 /*53*/:
                                case R.styleable.ChartTheme_sc_seriesAreaColor6 /*54*/:
                                case R.styleable.ChartTheme_sc_seriesAreaGradientColor6 /*55*/:
                                case R.styleable.ChartTheme_sc_seriesAreaGradientTransparentColor6 /*56*/:
                                case R.styleable.ChartTheme_sc_pieDonutFlavorColor1 /*57*/:
                                    t = _parsePosNumber(i);
                                    break;
                                case 91:
                                    t = JsonToken.START_ARRAY;
                                    break;
                                case 102:
                                    _matchToken("false", 1);
                                    t = JsonToken.VALUE_FALSE;
                                    break;
                                case 110:
                                    _matchToken("null", 1);
                                    t = JsonToken.VALUE_NULL;
                                    break;
                                case 116:
                                    _matchToken("true", 1);
                                    t = JsonToken.VALUE_TRUE;
                                    break;
                                case 123:
                                    t = JsonToken.START_OBJECT;
                                    break;
                                default:
                                    t = _handleUnexpectedValue(i);
                                    break;
                            }
                            this._nextToken = t;
                        }
                    } else {
                        _updateLocation();
                        _nextTokenNotInObject(i);
                    }
                }
            }
        }
        return str;
    }

    private final int _skipColonFast(int ptr) throws IOException {
        int ptr2 = ptr + 1;
        int i = this._inputBuffer[ptr];
        if (i == 58) {
            ptr = ptr2 + 1;
            i = this._inputBuffer[ptr2];
            if (i > 32) {
                if (!(i == 47 || i == 35)) {
                    this._inputPtr = ptr;
                    return i;
                }
            } else if (i == 32 || i == 9) {
                ptr2 = ptr + 1;
                i = this._inputBuffer[ptr];
                if (i <= 32 || i == 47 || i == 35) {
                    ptr = ptr2;
                } else {
                    this._inputPtr = ptr2;
                    ptr = ptr2;
                    return i;
                }
            }
            this._inputPtr = ptr - 1;
            return _skipColon2(true);
        }
        if (i == 32 || i == 9) {
            ptr = ptr2 + 1;
            i = this._inputBuffer[ptr2];
            ptr2 = ptr;
        }
        if (i == 58) {
            ptr = ptr2 + 1;
            i = this._inputBuffer[ptr2];
            if (i > 32) {
                if (!(i == 47 || i == 35)) {
                    this._inputPtr = ptr;
                    return i;
                }
            } else if (i == 32 || i == 9) {
                ptr2 = ptr + 1;
                i = this._inputBuffer[ptr];
                if (i <= 32 || i == 47 || i == 35) {
                    ptr = ptr2;
                } else {
                    this._inputPtr = ptr2;
                    ptr = ptr2;
                    return i;
                }
            }
            this._inputPtr = ptr - 1;
            return _skipColon2(true);
        }
        this._inputPtr = ptr2 - 1;
        ptr = ptr2;
        return _skipColon2(false);
    }

    private final void _isNextTokenNameYes(int i) throws IOException {
        this._currToken = JsonToken.FIELD_NAME;
        _updateLocation();
        switch (i) {
            case R.styleable.ChartTheme_sc_seriesAreaColor1 /*34*/:
                this._tokenIncomplete = true;
                this._nextToken = JsonToken.VALUE_STRING;
                return;
            case R.styleable.ChartTheme_sc_seriesLineColor4 /*45*/:
                this._nextToken = _parseNegNumber();
                return;
            case R.styleable.ChartTheme_sc_seriesAreaGradientTransparentColor4 /*48*/:
            case R.styleable.ChartTheme_sc_seriesLineColor5 /*49*/:
            case 50:
            case R.styleable.ChartTheme_sc_seriesAreaGradientColor5 /*51*/:
            case R.styleable.ChartTheme_sc_seriesAreaGradientTransparentColor5 /*52*/:
            case R.styleable.ChartTheme_sc_seriesLineColor6 /*53*/:
            case R.styleable.ChartTheme_sc_seriesAreaColor6 /*54*/:
            case R.styleable.ChartTheme_sc_seriesAreaGradientColor6 /*55*/:
            case R.styleable.ChartTheme_sc_seriesAreaGradientTransparentColor6 /*56*/:
            case R.styleable.ChartTheme_sc_pieDonutFlavorColor1 /*57*/:
                this._nextToken = _parsePosNumber(i);
                return;
            case 91:
                this._nextToken = JsonToken.START_ARRAY;
                return;
            case 102:
                _matchToken("false", 1);
                this._nextToken = JsonToken.VALUE_FALSE;
                return;
            case 110:
                _matchToken("null", 1);
                this._nextToken = JsonToken.VALUE_NULL;
                return;
            case 116:
                _matchToken("true", 1);
                this._nextToken = JsonToken.VALUE_TRUE;
                return;
            case 123:
                this._nextToken = JsonToken.START_OBJECT;
                return;
            default:
                this._nextToken = _handleUnexpectedValue(i);
                return;
        }
    }

    private final boolean _isNextTokenNameMaybe(int i, SerializableString str) throws IOException {
        String n = _parseName(i);
        this._parsingContext.setCurrentName(n);
        boolean match = n.equals(str.getValue());
        this._currToken = JsonToken.FIELD_NAME;
        i = _skipColon();
        _updateLocation();
        if (i == 34) {
            this._tokenIncomplete = true;
            this._nextToken = JsonToken.VALUE_STRING;
        } else {
            JsonToken t;
            switch (i) {
                case R.styleable.ChartTheme_sc_seriesLineColor4 /*45*/:
                    t = _parseNegNumber();
                    break;
                case R.styleable.ChartTheme_sc_seriesAreaGradientTransparentColor4 /*48*/:
                case R.styleable.ChartTheme_sc_seriesLineColor5 /*49*/:
                case 50:
                case R.styleable.ChartTheme_sc_seriesAreaGradientColor5 /*51*/:
                case R.styleable.ChartTheme_sc_seriesAreaGradientTransparentColor5 /*52*/:
                case R.styleable.ChartTheme_sc_seriesLineColor6 /*53*/:
                case R.styleable.ChartTheme_sc_seriesAreaColor6 /*54*/:
                case R.styleable.ChartTheme_sc_seriesAreaGradientColor6 /*55*/:
                case R.styleable.ChartTheme_sc_seriesAreaGradientTransparentColor6 /*56*/:
                case R.styleable.ChartTheme_sc_pieDonutFlavorColor1 /*57*/:
                    t = _parsePosNumber(i);
                    break;
                case 91:
                    t = JsonToken.START_ARRAY;
                    break;
                case 102:
                    _matchToken("false", 1);
                    t = JsonToken.VALUE_FALSE;
                    break;
                case 110:
                    _matchToken("null", 1);
                    t = JsonToken.VALUE_NULL;
                    break;
                case 116:
                    _matchToken("true", 1);
                    t = JsonToken.VALUE_TRUE;
                    break;
                case 123:
                    t = JsonToken.START_OBJECT;
                    break;
                default:
                    t = _handleUnexpectedValue(i);
                    break;
            }
            this._nextToken = t;
        }
        return match;
    }

    public String nextTextValue() throws IOException {
        if (this._currToken == JsonToken.FIELD_NAME) {
            this._nameCopied = false;
            JsonToken t = this._nextToken;
            this._nextToken = null;
            this._currToken = t;
            if (t == JsonToken.VALUE_STRING) {
                if (!this._tokenIncomplete) {
                    return this._textBuffer.contentsAsString();
                }
                this._tokenIncomplete = false;
                return _finishAndReturnString();
            } else if (t == JsonToken.START_ARRAY) {
                this._parsingContext = this._parsingContext.createChildArrayContext(this._tokenInputRow, this._tokenInputCol);
                return null;
            } else if (t != JsonToken.START_OBJECT) {
                return null;
            } else {
                this._parsingContext = this._parsingContext.createChildObjectContext(this._tokenInputRow, this._tokenInputCol);
                return null;
            }
        } else if (nextToken() == JsonToken.VALUE_STRING) {
            return getText();
        } else {
            return null;
        }
    }

    public int nextIntValue(int defaultValue) throws IOException {
        if (this._currToken != JsonToken.FIELD_NAME) {
            return nextToken() == JsonToken.VALUE_NUMBER_INT ? getIntValue() : defaultValue;
        } else {
            this._nameCopied = false;
            JsonToken t = this._nextToken;
            this._nextToken = null;
            this._currToken = t;
            if (t == JsonToken.VALUE_NUMBER_INT) {
                return getIntValue();
            }
            if (t == JsonToken.START_ARRAY) {
                this._parsingContext = this._parsingContext.createChildArrayContext(this._tokenInputRow, this._tokenInputCol);
                return defaultValue;
            } else if (t != JsonToken.START_OBJECT) {
                return defaultValue;
            } else {
                this._parsingContext = this._parsingContext.createChildObjectContext(this._tokenInputRow, this._tokenInputCol);
                return defaultValue;
            }
        }
    }

    public long nextLongValue(long defaultValue) throws IOException {
        if (this._currToken != JsonToken.FIELD_NAME) {
            return nextToken() == JsonToken.VALUE_NUMBER_INT ? getLongValue() : defaultValue;
        } else {
            this._nameCopied = false;
            JsonToken t = this._nextToken;
            this._nextToken = null;
            this._currToken = t;
            if (t == JsonToken.VALUE_NUMBER_INT) {
                return getLongValue();
            }
            if (t == JsonToken.START_ARRAY) {
                this._parsingContext = this._parsingContext.createChildArrayContext(this._tokenInputRow, this._tokenInputCol);
                return defaultValue;
            } else if (t != JsonToken.START_OBJECT) {
                return defaultValue;
            } else {
                this._parsingContext = this._parsingContext.createChildObjectContext(this._tokenInputRow, this._tokenInputCol);
                return defaultValue;
            }
        }
    }

    public Boolean nextBooleanValue() throws IOException {
        JsonToken t;
        if (this._currToken == JsonToken.FIELD_NAME) {
            this._nameCopied = false;
            t = this._nextToken;
            this._nextToken = null;
            this._currToken = t;
            if (t == JsonToken.VALUE_TRUE) {
                return Boolean.TRUE;
            }
            if (t == JsonToken.VALUE_FALSE) {
                return Boolean.FALSE;
            }
            if (t == JsonToken.START_ARRAY) {
                this._parsingContext = this._parsingContext.createChildArrayContext(this._tokenInputRow, this._tokenInputCol);
                return null;
            } else if (t != JsonToken.START_OBJECT) {
                return null;
            } else {
                this._parsingContext = this._parsingContext.createChildObjectContext(this._tokenInputRow, this._tokenInputCol);
                return null;
            }
        }
        t = nextToken();
        if (t == JsonToken.VALUE_TRUE) {
            return Boolean.TRUE;
        }
        if (t == JsonToken.VALUE_FALSE) {
            return Boolean.FALSE;
        }
        return null;
    }

    protected JsonToken _parsePosNumber(int c) throws IOException {
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        if (c == 48) {
            c = _verifyNoLeadingZeroes();
        }
        outBuf[0] = (char) c;
        int intLen = 1;
        int outPtr = 1;
        int end = (this._inputPtr + outBuf.length) - 1;
        if (end > this._inputEnd) {
            end = this._inputEnd;
        }
        while (this._inputPtr < end) {
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            c = bArr[i] & 255;
            if (c >= 48 && c <= 57) {
                intLen++;
                int outPtr2 = outPtr + 1;
                outBuf[outPtr] = (char) c;
                outPtr = outPtr2;
            } else if (c == 46 || c == 101 || c == 69) {
                return _parseFloat(outBuf, outPtr, c, false, intLen);
            } else {
                this._inputPtr--;
                this._textBuffer.setCurrentLength(outPtr);
                if (this._parsingContext.inRoot()) {
                    _verifyRootSpace(c);
                }
                return resetInt(false, intLen);
            }
        }
        return _parseNumber2(outBuf, outPtr, false, intLen);
    }

    protected JsonToken _parseNegNumber() throws IOException {
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        int outPtr = 0 + 1;
        outBuf[0] = '-';
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int c = bArr[i] & 255;
        if (c < 48 || c > 57) {
            return _handleInvalidNumberStart(c, true);
        }
        if (c == 48) {
            c = _verifyNoLeadingZeroes();
        }
        outPtr++;
        outBuf[1] = (char) c;
        int intLen = 1;
        int end = (this._inputPtr + outBuf.length) - 2;
        if (end > this._inputEnd) {
            end = this._inputEnd;
        }
        while (this._inputPtr < end) {
            bArr = this._inputBuffer;
            i = this._inputPtr;
            this._inputPtr = i + 1;
            c = bArr[i] & 255;
            if (c >= 48 && c <= 57) {
                intLen++;
                int outPtr2 = outPtr + 1;
                outBuf[outPtr] = (char) c;
                outPtr = outPtr2;
            } else if (c == 46 || c == 101 || c == 69) {
                return _parseFloat(outBuf, outPtr, c, true, intLen);
            } else {
                this._inputPtr--;
                this._textBuffer.setCurrentLength(outPtr);
                if (this._parsingContext.inRoot()) {
                    _verifyRootSpace(c);
                }
                return resetInt(true, intLen);
            }
        }
        return _parseNumber2(outBuf, outPtr, true, intLen);
    }

    private final JsonToken _parseNumber2(char[] outBuf, int outPtr, boolean negative, int intPartLength) throws IOException {
        int c;
        while (true) {
            if (this._inputPtr < this._inputEnd || _loadMore()) {
                byte[] bArr = this._inputBuffer;
                int i = this._inputPtr;
                this._inputPtr = i + 1;
                c = bArr[i] & 255;
                if (c <= 57 && c >= 48) {
                    if (outPtr >= outBuf.length) {
                        outBuf = this._textBuffer.finishCurrentSegment();
                        outPtr = 0;
                    }
                    int outPtr2 = outPtr + 1;
                    outBuf[outPtr] = (char) c;
                    intPartLength++;
                    outPtr = outPtr2;
                }
            } else {
                this._textBuffer.setCurrentLength(outPtr);
                return resetInt(negative, intPartLength);
            }
        }
        if (c == 46 || c == 101 || c == 69) {
            return _parseFloat(outBuf, outPtr, c, negative, intPartLength);
        }
        this._inputPtr--;
        this._textBuffer.setCurrentLength(outPtr);
        if (this._parsingContext.inRoot()) {
            bArr = this._inputBuffer;
            i = this._inputPtr;
            this._inputPtr = i + 1;
            _verifyRootSpace(bArr[i] & 255);
        }
        return resetInt(negative, intPartLength);
    }

    private final int _verifyNoLeadingZeroes() throws IOException {
        if (this._inputPtr >= this._inputEnd && !_loadMore()) {
            return 48;
        }
        int ch = this._inputBuffer[this._inputPtr] & 255;
        if (ch < 48 || ch > 57) {
            return 48;
        }
        if (!isEnabled(Feature.ALLOW_NUMERIC_LEADING_ZEROS)) {
            reportInvalidNumber("Leading zeroes not allowed");
        }
        this._inputPtr++;
        if (ch != 48) {
            return ch;
        }
        do {
            if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                return ch;
            }
            ch = this._inputBuffer[this._inputPtr] & 255;
            if (ch < 48 || ch > 57) {
                return 48;
            }
            this._inputPtr++;
        } while (ch == 48);
        return ch;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private final com.fasterxml.jackson.core.JsonToken _parseFloat(char[] r10, int r11, int r12, boolean r13, int r14) throws java.io.IOException {
        /*
        r9 = this;
        r8 = 57;
        r7 = 48;
        r2 = 0;
        r0 = 0;
        r4 = 46;
        if (r12 != r4) goto L_0x002e;
    L_0x000a:
        r4 = r10.length;
        if (r11 < r4) goto L_0x0014;
    L_0x000d:
        r4 = r9._textBuffer;
        r10 = r4.finishCurrentSegment();
        r11 = 0;
    L_0x0014:
        r3 = r11 + 1;
        r4 = (char) r12;
        r10[r11] = r4;
        r11 = r3;
    L_0x001a:
        r4 = r9._inputPtr;
        r5 = r9._inputEnd;
        if (r4 < r5) goto L_0x00cf;
    L_0x0020:
        r4 = r9._loadMore();
        if (r4 != 0) goto L_0x00cf;
    L_0x0026:
        r0 = 1;
    L_0x0027:
        if (r2 != 0) goto L_0x002e;
    L_0x0029:
        r4 = "Decimal point not followed by a digit";
        r9.reportUnexpectedNumberChar(r12, r4);
    L_0x002e:
        r1 = 0;
        r4 = 101; // 0x65 float:1.42E-43 double:5.0E-322;
        if (r12 == r4) goto L_0x0037;
    L_0x0033:
        r4 = 69;
        if (r12 != r4) goto L_0x00b2;
    L_0x0037:
        r4 = r10.length;
        if (r11 < r4) goto L_0x0041;
    L_0x003a:
        r4 = r9._textBuffer;
        r10 = r4.finishCurrentSegment();
        r11 = 0;
    L_0x0041:
        r3 = r11 + 1;
        r4 = (char) r12;
        r10[r11] = r4;
        r4 = r9._inputPtr;
        r5 = r9._inputEnd;
        if (r4 < r5) goto L_0x004f;
    L_0x004c:
        r9._loadMoreGuaranteed();
    L_0x004f:
        r4 = r9._inputBuffer;
        r5 = r9._inputPtr;
        r6 = r5 + 1;
        r9._inputPtr = r6;
        r4 = r4[r5];
        r12 = r4 & 255;
        r4 = 45;
        if (r12 == r4) goto L_0x0063;
    L_0x005f:
        r4 = 43;
        if (r12 != r4) goto L_0x0104;
    L_0x0063:
        r4 = r10.length;
        if (r3 < r4) goto L_0x0101;
    L_0x0066:
        r4 = r9._textBuffer;
        r10 = r4.finishCurrentSegment();
        r11 = 0;
    L_0x006d:
        r3 = r11 + 1;
        r4 = (char) r12;
        r10[r11] = r4;
        r4 = r9._inputPtr;
        r5 = r9._inputEnd;
        if (r4 < r5) goto L_0x007b;
    L_0x0078:
        r9._loadMoreGuaranteed();
    L_0x007b:
        r4 = r9._inputBuffer;
        r5 = r9._inputPtr;
        r6 = r5 + 1;
        r9._inputPtr = r6;
        r4 = r4[r5];
        r12 = r4 & 255;
        r11 = r3;
    L_0x0088:
        if (r12 > r8) goto L_0x00ab;
    L_0x008a:
        if (r12 < r7) goto L_0x00ab;
    L_0x008c:
        r1 = r1 + 1;
        r4 = r10.length;
        if (r11 < r4) goto L_0x0098;
    L_0x0091:
        r4 = r9._textBuffer;
        r10 = r4.finishCurrentSegment();
        r11 = 0;
    L_0x0098:
        r3 = r11 + 1;
        r4 = (char) r12;
        r10[r11] = r4;
        r4 = r9._inputPtr;
        r5 = r9._inputEnd;
        if (r4 < r5) goto L_0x00f3;
    L_0x00a3:
        r4 = r9._loadMore();
        if (r4 != 0) goto L_0x00f3;
    L_0x00a9:
        r0 = 1;
        r11 = r3;
    L_0x00ab:
        if (r1 != 0) goto L_0x00b2;
    L_0x00ad:
        r4 = "Exponent indicator not followed by a digit";
        r9.reportUnexpectedNumberChar(r12, r4);
    L_0x00b2:
        if (r0 != 0) goto L_0x00c5;
    L_0x00b4:
        r4 = r9._inputPtr;
        r4 = r4 + -1;
        r9._inputPtr = r4;
        r4 = r9._parsingContext;
        r4 = r4.inRoot();
        if (r4 == 0) goto L_0x00c5;
    L_0x00c2:
        r9._verifyRootSpace(r12);
    L_0x00c5:
        r4 = r9._textBuffer;
        r4.setCurrentLength(r11);
        r4 = r9.resetFloat(r13, r14, r2, r1);
        return r4;
    L_0x00cf:
        r4 = r9._inputBuffer;
        r5 = r9._inputPtr;
        r6 = r5 + 1;
        r9._inputPtr = r6;
        r4 = r4[r5];
        r12 = r4 & 255;
        if (r12 < r7) goto L_0x0027;
    L_0x00dd:
        if (r12 > r8) goto L_0x0027;
    L_0x00df:
        r2 = r2 + 1;
        r4 = r10.length;
        if (r11 < r4) goto L_0x00eb;
    L_0x00e4:
        r4 = r9._textBuffer;
        r10 = r4.finishCurrentSegment();
        r11 = 0;
    L_0x00eb:
        r3 = r11 + 1;
        r4 = (char) r12;
        r10[r11] = r4;
        r11 = r3;
        goto L_0x001a;
    L_0x00f3:
        r4 = r9._inputBuffer;
        r5 = r9._inputPtr;
        r6 = r5 + 1;
        r9._inputPtr = r6;
        r4 = r4[r5];
        r12 = r4 & 255;
        r11 = r3;
        goto L_0x0088;
    L_0x0101:
        r11 = r3;
        goto L_0x006d;
    L_0x0104:
        r11 = r3;
        goto L_0x0088;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.fasterxml.jackson.core.json.UTF8StreamJsonParser._parseFloat(char[], int, int, boolean, int):com.fasterxml.jackson.core.JsonToken");
    }

    private final void _verifyRootSpace(int ch) throws IOException {
        this._inputPtr++;
        switch (ch) {
            case 9:
            case 32:
                return;
            case 10:
                this._currInputRow++;
                this._currInputRowStart = this._inputPtr;
                return;
            case 13:
                _skipCR();
                return;
            default:
                _reportMissingRootWS(ch);
                return;
        }
    }

    protected final String _parseName(int i) throws IOException {
        if (i != 34) {
            return _handleOddName(i);
        }
        if (this._inputPtr + 13 > this._inputEnd) {
            return slowParseName();
        }
        byte[] input = this._inputBuffer;
        int[] codes = _icLatin1;
        int i2 = this._inputPtr;
        this._inputPtr = i2 + 1;
        int q = input[i2] & 255;
        if (codes[q] == 0) {
            i2 = this._inputPtr;
            this._inputPtr = i2 + 1;
            i = input[i2] & 255;
            if (codes[i] == 0) {
                q = (q << 8) | i;
                i2 = this._inputPtr;
                this._inputPtr = i2 + 1;
                i = input[i2] & 255;
                if (codes[i] == 0) {
                    q = (q << 8) | i;
                    i2 = this._inputPtr;
                    this._inputPtr = i2 + 1;
                    i = input[i2] & 255;
                    if (codes[i] == 0) {
                        q = (q << 8) | i;
                        i2 = this._inputPtr;
                        this._inputPtr = i2 + 1;
                        i = input[i2] & 255;
                        if (codes[i] == 0) {
                            this._quad1 = q;
                            return parseMediumName(i);
                        } else if (i == 34) {
                            return findName(q, 4);
                        } else {
                            return parseName(q, i, 4);
                        }
                    } else if (i == 34) {
                        return findName(q, 3);
                    } else {
                        return parseName(q, i, 3);
                    }
                } else if (i == 34) {
                    return findName(q, 2);
                } else {
                    return parseName(q, i, 2);
                }
            } else if (i == 34) {
                return findName(q, 1);
            } else {
                return parseName(q, i, 1);
            }
        } else if (q == 34) {
            return "";
        } else {
            return parseName(0, q, 0);
        }
    }

    protected final String parseMediumName(int q2) throws IOException {
        byte[] input = this._inputBuffer;
        int[] codes = _icLatin1;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int i2 = input[i] & 255;
        if (codes[i2] == 0) {
            q2 = (q2 << 8) | i2;
            i = this._inputPtr;
            this._inputPtr = i + 1;
            i2 = input[i] & 255;
            if (codes[i2] == 0) {
                q2 = (q2 << 8) | i2;
                i = this._inputPtr;
                this._inputPtr = i + 1;
                i2 = input[i] & 255;
                if (codes[i2] == 0) {
                    q2 = (q2 << 8) | i2;
                    i = this._inputPtr;
                    this._inputPtr = i + 1;
                    i2 = input[i] & 255;
                    if (codes[i2] == 0) {
                        return parseMediumName2(i2, q2);
                    }
                    if (i2 == 34) {
                        return findName(this._quad1, q2, 4);
                    }
                    return parseName(this._quad1, q2, i2, 4);
                } else if (i2 == 34) {
                    return findName(this._quad1, q2, 3);
                } else {
                    return parseName(this._quad1, q2, i2, 3);
                }
            } else if (i2 == 34) {
                return findName(this._quad1, q2, 2);
            } else {
                return parseName(this._quad1, q2, i2, 2);
            }
        } else if (i2 == 34) {
            return findName(this._quad1, q2, 1);
        } else {
            return parseName(this._quad1, q2, i2, 1);
        }
    }

    protected final String parseMediumName2(int q3, int q2) throws IOException {
        byte[] input = this._inputBuffer;
        int[] codes = _icLatin1;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int i2 = input[i] & 255;
        if (codes[i2] == 0) {
            q3 = (q3 << 8) | i2;
            i = this._inputPtr;
            this._inputPtr = i + 1;
            i2 = input[i] & 255;
            if (codes[i2] == 0) {
                q3 = (q3 << 8) | i2;
                i = this._inputPtr;
                this._inputPtr = i + 1;
                i2 = input[i] & 255;
                if (codes[i2] == 0) {
                    q3 = (q3 << 8) | i2;
                    i = this._inputPtr;
                    this._inputPtr = i + 1;
                    i2 = input[i] & 255;
                    if (codes[i2] == 0) {
                        return parseLongName(i2, q2, q3);
                    }
                    if (i2 == 34) {
                        return findName(this._quad1, q2, q3, 4);
                    }
                    return parseName(this._quad1, q2, q3, i2, 4);
                } else if (i2 == 34) {
                    return findName(this._quad1, q2, q3, 3);
                } else {
                    return parseName(this._quad1, q2, q3, i2, 3);
                }
            } else if (i2 == 34) {
                return findName(this._quad1, q2, q3, 2);
            } else {
                return parseName(this._quad1, q2, q3, i2, 2);
            }
        } else if (i2 == 34) {
            return findName(this._quad1, q2, q3, 1);
        } else {
            return parseName(this._quad1, q2, q3, i2, 1);
        }
    }

    protected final String parseLongName(int q, int q2, int q3) throws IOException {
        this._quadBuffer[0] = this._quad1;
        this._quadBuffer[1] = q2;
        this._quadBuffer[2] = q3;
        byte[] input = this._inputBuffer;
        int[] codes = _icLatin1;
        int qlen = 3;
        while (this._inputPtr + 4 <= this._inputEnd) {
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int i2 = input[i] & 255;
            if (codes[i2] == 0) {
                q = (q << 8) | i2;
                i = this._inputPtr;
                this._inputPtr = i + 1;
                i2 = input[i] & 255;
                if (codes[i2] == 0) {
                    q = (q << 8) | i2;
                    i = this._inputPtr;
                    this._inputPtr = i + 1;
                    i2 = input[i] & 255;
                    if (codes[i2] == 0) {
                        q = (q << 8) | i2;
                        i = this._inputPtr;
                        this._inputPtr = i + 1;
                        i2 = input[i] & 255;
                        if (codes[i2] == 0) {
                            if (qlen >= this._quadBuffer.length) {
                                this._quadBuffer = growArrayBy(this._quadBuffer, qlen);
                            }
                            int qlen2 = qlen + 1;
                            this._quadBuffer[qlen] = q;
                            q = i2;
                            qlen = qlen2;
                        } else if (i2 == 34) {
                            return findName(this._quadBuffer, qlen, q, 4);
                        } else {
                            return parseEscapedName(this._quadBuffer, qlen, q, i2, 4);
                        }
                    } else if (i2 == 34) {
                        return findName(this._quadBuffer, qlen, q, 3);
                    } else {
                        return parseEscapedName(this._quadBuffer, qlen, q, i2, 3);
                    }
                } else if (i2 == 34) {
                    return findName(this._quadBuffer, qlen, q, 2);
                } else {
                    return parseEscapedName(this._quadBuffer, qlen, q, i2, 2);
                }
            } else if (i2 == 34) {
                return findName(this._quadBuffer, qlen, q, 1);
            } else {
                return parseEscapedName(this._quadBuffer, qlen, q, i2, 1);
            }
        }
        return parseEscapedName(this._quadBuffer, qlen, 0, q, 0);
    }

    protected String slowParseName() throws IOException {
        if (this._inputPtr >= this._inputEnd && !_loadMore()) {
            _reportInvalidEOF(": was expecting closing '\"' for name", JsonToken.FIELD_NAME);
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int i2 = bArr[i] & 255;
        if (i2 == 34) {
            return "";
        }
        return parseEscapedName(this._quadBuffer, 0, 0, i2, 0);
    }

    private final String parseName(int q1, int ch, int lastQuadBytes) throws IOException {
        return parseEscapedName(this._quadBuffer, 0, q1, ch, lastQuadBytes);
    }

    private final String parseName(int q1, int q2, int ch, int lastQuadBytes) throws IOException {
        this._quadBuffer[0] = q1;
        return parseEscapedName(this._quadBuffer, 1, q2, ch, lastQuadBytes);
    }

    private final String parseName(int q1, int q2, int q3, int ch, int lastQuadBytes) throws IOException {
        this._quadBuffer[0] = q1;
        this._quadBuffer[1] = q2;
        return parseEscapedName(this._quadBuffer, 2, q3, ch, lastQuadBytes);
    }

    protected final String parseEscapedName(int[] quads, int qlen, int currQuad, int ch, int currQuadBytes) throws IOException {
        int[] codes = _icLatin1;
        while (true) {
            int qlen2;
            byte[] bArr;
            int i;
            if (codes[ch] != 0) {
                if (ch == 34) {
                    break;
                }
                if (ch != 92) {
                    _throwUnquotedSpace(ch, "name");
                } else {
                    ch = _decodeEscaped();
                }
                if (ch > 127) {
                    if (currQuadBytes >= 4) {
                        if (qlen >= quads.length) {
                            quads = growArrayBy(quads, quads.length);
                            this._quadBuffer = quads;
                        }
                        qlen2 = qlen + 1;
                        quads[qlen] = currQuad;
                        currQuad = 0;
                        currQuadBytes = 0;
                    } else {
                        qlen2 = qlen;
                    }
                    if (ch < ItemAnimator.FLAG_MOVED) {
                        currQuad = (currQuad << 8) | ((ch >> 6) | 192);
                        currQuadBytes++;
                        qlen = qlen2;
                    } else {
                        currQuad = (currQuad << 8) | ((ch >> 12) | 224);
                        currQuadBytes++;
                        if (currQuadBytes >= 4) {
                            if (qlen2 >= quads.length) {
                                quads = growArrayBy(quads, quads.length);
                                this._quadBuffer = quads;
                            }
                            qlen = qlen2 + 1;
                            quads[qlen2] = currQuad;
                            currQuad = 0;
                            currQuadBytes = 0;
                        } else {
                            qlen = qlen2;
                        }
                        currQuad = (currQuad << 8) | (((ch >> 6) & 63) | 128);
                        currQuadBytes++;
                    }
                    ch = (ch & 63) | 128;
                    qlen2 = qlen;
                    if (currQuadBytes >= 4) {
                        currQuadBytes++;
                        currQuad = (currQuad << 8) | ch;
                        qlen = qlen2;
                    } else {
                        if (qlen2 >= quads.length) {
                            quads = growArrayBy(quads, quads.length);
                            this._quadBuffer = quads;
                        }
                        qlen = qlen2 + 1;
                        quads[qlen2] = currQuad;
                        currQuad = ch;
                        currQuadBytes = 1;
                    }
                    if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                        _reportInvalidEOF(" in field name", JsonToken.FIELD_NAME);
                    }
                    bArr = this._inputBuffer;
                    i = this._inputPtr;
                    this._inputPtr = i + 1;
                    ch = bArr[i] & 255;
                }
            }
            qlen2 = qlen;
            if (currQuadBytes >= 4) {
                if (qlen2 >= quads.length) {
                    quads = growArrayBy(quads, quads.length);
                    this._quadBuffer = quads;
                }
                qlen = qlen2 + 1;
                quads[qlen2] = currQuad;
                currQuad = ch;
                currQuadBytes = 1;
            } else {
                currQuadBytes++;
                currQuad = (currQuad << 8) | ch;
                qlen = qlen2;
            }
            _reportInvalidEOF(" in field name", JsonToken.FIELD_NAME);
            bArr = this._inputBuffer;
            i = this._inputPtr;
            this._inputPtr = i + 1;
            ch = bArr[i] & 255;
        }
        if (currQuadBytes > 0) {
            if (qlen >= quads.length) {
                quads = growArrayBy(quads, quads.length);
                this._quadBuffer = quads;
            }
            qlen2 = qlen + 1;
            quads[qlen] = pad(currQuad, currQuadBytes);
            qlen = qlen2;
        }
        String name = this._symbols.findName(quads, qlen);
        if (name == null) {
            return addName(quads, qlen, currQuadBytes);
        }
        return name;
    }

    protected String _handleOddName(int ch) throws IOException {
        if (ch == 39 && isEnabled(Feature.ALLOW_SINGLE_QUOTES)) {
            return _parseAposName();
        }
        int qlen;
        if (!isEnabled(Feature.ALLOW_UNQUOTED_FIELD_NAMES)) {
            _reportUnexpectedChar((char) _decodeCharForError(ch), "was expecting double-quote to start field name");
        }
        int[] codes = CharTypes.getInputCodeUtf8JsNames();
        if (codes[ch] != 0) {
            _reportUnexpectedChar(ch, "was expecting either valid name character (for unquoted name) or double-quote (for quoted) to start field name");
        }
        int[] quads = this._quadBuffer;
        int currQuad = 0;
        int currQuadBytes = 0;
        int qlen2 = 0;
        while (true) {
            if (currQuadBytes < 4) {
                currQuadBytes++;
                currQuad = (currQuad << 8) | ch;
                qlen = qlen2;
            } else {
                if (qlen2 >= quads.length) {
                    quads = growArrayBy(quads, quads.length);
                    this._quadBuffer = quads;
                }
                qlen = qlen2 + 1;
                quads[qlen2] = currQuad;
                currQuad = ch;
                currQuadBytes = 1;
            }
            if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                _reportInvalidEOF(" in field name", JsonToken.FIELD_NAME);
            }
            ch = this._inputBuffer[this._inputPtr] & 255;
            if (codes[ch] != 0) {
                break;
            }
            this._inputPtr++;
            qlen2 = qlen;
        }
        if (currQuadBytes > 0) {
            if (qlen >= quads.length) {
                quads = growArrayBy(quads, quads.length);
                this._quadBuffer = quads;
            }
            qlen2 = qlen + 1;
            quads[qlen] = currQuad;
            qlen = qlen2;
        }
        String name = this._symbols.findName(quads, qlen);
        if (name == null) {
            return addName(quads, qlen, currQuadBytes);
        }
        return name;
    }

    protected String _parseAposName() throws IOException {
        if (this._inputPtr >= this._inputEnd && !_loadMore()) {
            _reportInvalidEOF(": was expecting closing ''' for field name", JsonToken.FIELD_NAME);
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int ch = bArr[i] & 255;
        if (ch == 39) {
            return "";
        }
        int qlen;
        int[] quads = this._quadBuffer;
        int currQuad = 0;
        int currQuadBytes = 0;
        int[] codes = _icLatin1;
        int qlen2 = 0;
        while (ch != 39) {
            if (!(ch == 34 || codes[ch] == 0)) {
                if (ch != 92) {
                    _throwUnquotedSpace(ch, "name");
                } else {
                    ch = _decodeEscaped();
                }
                if (ch > 127) {
                    if (currQuadBytes >= 4) {
                        if (qlen2 >= quads.length) {
                            quads = growArrayBy(quads, quads.length);
                            this._quadBuffer = quads;
                        }
                        qlen = qlen2 + 1;
                        quads[qlen2] = currQuad;
                        currQuad = 0;
                        currQuadBytes = 0;
                        qlen2 = qlen;
                    }
                    if (ch < ItemAnimator.FLAG_MOVED) {
                        currQuad = (currQuad << 8) | ((ch >> 6) | 192);
                        currQuadBytes++;
                        qlen = qlen2;
                    } else {
                        currQuad = (currQuad << 8) | ((ch >> 12) | 224);
                        currQuadBytes++;
                        if (currQuadBytes >= 4) {
                            if (qlen2 >= quads.length) {
                                quads = growArrayBy(quads, quads.length);
                                this._quadBuffer = quads;
                            }
                            qlen = qlen2 + 1;
                            quads[qlen2] = currQuad;
                            currQuad = 0;
                            currQuadBytes = 0;
                        } else {
                            qlen = qlen2;
                        }
                        currQuad = (currQuad << 8) | (((ch >> 6) & 63) | 128);
                        currQuadBytes++;
                    }
                    ch = (ch & 63) | 128;
                    qlen2 = qlen;
                }
            }
            if (currQuadBytes < 4) {
                currQuadBytes++;
                currQuad = (currQuad << 8) | ch;
                qlen = qlen2;
            } else {
                if (qlen2 >= quads.length) {
                    quads = growArrayBy(quads, quads.length);
                    this._quadBuffer = quads;
                }
                qlen = qlen2 + 1;
                quads[qlen2] = currQuad;
                currQuad = ch;
                currQuadBytes = 1;
            }
            if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                _reportInvalidEOF(" in field name", JsonToken.FIELD_NAME);
            }
            bArr = this._inputBuffer;
            i = this._inputPtr;
            this._inputPtr = i + 1;
            ch = bArr[i] & 255;
            qlen2 = qlen;
        }
        if (currQuadBytes > 0) {
            if (qlen2 >= quads.length) {
                quads = growArrayBy(quads, quads.length);
                this._quadBuffer = quads;
            }
            qlen = qlen2 + 1;
            quads[qlen2] = pad(currQuad, currQuadBytes);
        } else {
            qlen = qlen2;
        }
        String name = this._symbols.findName(quads, qlen);
        if (name == null) {
            return addName(quads, qlen, currQuadBytes);
        }
        return name;
    }

    private final String findName(int q1, int lastQuadBytes) throws JsonParseException {
        q1 = pad(q1, lastQuadBytes);
        String name = this._symbols.findName(q1);
        if (name != null) {
            return name;
        }
        this._quadBuffer[0] = q1;
        return addName(this._quadBuffer, 1, lastQuadBytes);
    }

    private final String findName(int q1, int q2, int lastQuadBytes) throws JsonParseException {
        q2 = pad(q2, lastQuadBytes);
        String name = this._symbols.findName(q1, q2);
        if (name != null) {
            return name;
        }
        this._quadBuffer[0] = q1;
        this._quadBuffer[1] = q2;
        return addName(this._quadBuffer, 2, lastQuadBytes);
    }

    private final String findName(int q1, int q2, int q3, int lastQuadBytes) throws JsonParseException {
        q3 = pad(q3, lastQuadBytes);
        String name = this._symbols.findName(q1, q2, q3);
        if (name != null) {
            return name;
        }
        int[] quads = this._quadBuffer;
        quads[0] = q1;
        quads[1] = q2;
        quads[2] = pad(q3, lastQuadBytes);
        return addName(quads, 3, lastQuadBytes);
    }

    private final String findName(int[] quads, int qlen, int lastQuad, int lastQuadBytes) throws JsonParseException {
        if (qlen >= quads.length) {
            quads = growArrayBy(quads, quads.length);
            this._quadBuffer = quads;
        }
        int qlen2 = qlen + 1;
        quads[qlen] = pad(lastQuad, lastQuadBytes);
        String name = this._symbols.findName(quads, qlen2);
        if (name == null) {
            return addName(quads, qlen2, lastQuadBytes);
        }
        return name;
    }

    private final String addName(int[] quads, int qlen, int lastQuadBytes) throws JsonParseException {
        int lastQuad;
        int byteLen = ((qlen << 2) - 4) + lastQuadBytes;
        if (lastQuadBytes < 4) {
            lastQuad = quads[qlen - 1];
            quads[qlen - 1] = lastQuad << ((4 - lastQuadBytes) << 3);
        } else {
            lastQuad = 0;
        }
        char[] cbuf = this._textBuffer.emptyAndGetCurrentSegment();
        int ix = 0;
        int cix = 0;
        while (ix < byteLen) {
            int i;
            int i2 = (quads[ix >> 2] >> ((3 - (ix & 3)) << 3)) & 255;
            ix++;
            if (i2 > 127) {
                int needed;
                if ((i2 & 224) == 192) {
                    i2 &= 31;
                    needed = 1;
                } else if ((i2 & 240) == 224) {
                    i2 &= 15;
                    needed = 2;
                } else if ((i2 & 248) == 240) {
                    i2 &= 7;
                    needed = 3;
                } else {
                    _reportInvalidInitial(i2);
                    i2 = 1;
                    needed = 1;
                }
                if (ix + needed > byteLen) {
                    _reportInvalidEOF(" in field name", JsonToken.FIELD_NAME);
                }
                int ch2 = quads[ix >> 2] >> ((3 - (ix & 3)) << 3);
                ix++;
                if ((ch2 & 192) != 128) {
                    _reportInvalidOther(ch2);
                }
                i2 = (i2 << 6) | (ch2 & 63);
                if (needed > 1) {
                    ch2 = quads[ix >> 2] >> ((3 - (ix & 3)) << 3);
                    ix++;
                    if ((ch2 & 192) != 128) {
                        _reportInvalidOther(ch2);
                    }
                    i2 = (i2 << 6) | (ch2 & 63);
                    if (needed > 2) {
                        ch2 = quads[ix >> 2] >> ((3 - (ix & 3)) << 3);
                        ix++;
                        if ((ch2 & 192) != 128) {
                            _reportInvalidOther(ch2 & 255);
                        }
                        i2 = (i2 << 6) | (ch2 & 63);
                    }
                }
                if (needed > 2) {
                    i2 -= 65536;
                    if (cix >= cbuf.length) {
                        cbuf = this._textBuffer.expandCurrentSegment();
                    }
                    i = cix + 1;
                    cbuf[cix] = (char) (55296 + (i2 >> 10));
                    i2 = 56320 | (i2 & 1023);
                    if (i >= cbuf.length) {
                        cbuf = this._textBuffer.expandCurrentSegment();
                    }
                    cix = i + 1;
                    cbuf[i] = (char) i2;
                }
            }
            i = cix;
            if (i >= cbuf.length) {
                cbuf = this._textBuffer.expandCurrentSegment();
            }
            cix = i + 1;
            cbuf[i] = (char) i2;
        }
        String baseName = new String(cbuf, 0, cix);
        if (lastQuadBytes < 4) {
            quads[qlen - 1] = lastQuad;
        }
        return this._symbols.addName(baseName, quads, qlen);
    }

    protected void _loadMoreGuaranteed() throws IOException {
        if (!_loadMore()) {
            _reportInvalidEOF();
        }
    }

    protected void _finishString() throws IOException {
        int ptr = this._inputPtr;
        if (ptr >= this._inputEnd) {
            _loadMoreGuaranteed();
            ptr = this._inputPtr;
        }
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        int[] codes = _icUTF8;
        int max = Math.min(this._inputEnd, outBuf.length + ptr);
        byte[] inputBuffer = this._inputBuffer;
        int outPtr = 0;
        while (ptr < max) {
            int c = inputBuffer[ptr] & 255;
            if (codes[c] != 0) {
                if (c == 34) {
                    this._inputPtr = ptr + 1;
                    this._textBuffer.setCurrentLength(outPtr);
                    return;
                }
                this._inputPtr = ptr;
                _finishString2(outBuf, outPtr);
            }
            ptr++;
            int outPtr2 = outPtr + 1;
            outBuf[outPtr] = (char) c;
            outPtr = outPtr2;
        }
        this._inputPtr = ptr;
        _finishString2(outBuf, outPtr);
    }

    protected String _finishAndReturnString() throws IOException {
        int ptr = this._inputPtr;
        if (ptr >= this._inputEnd) {
            _loadMoreGuaranteed();
            ptr = this._inputPtr;
        }
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        int[] codes = _icUTF8;
        int max = Math.min(this._inputEnd, outBuf.length + ptr);
        byte[] inputBuffer = this._inputBuffer;
        int outPtr = 0;
        while (ptr < max) {
            int c = inputBuffer[ptr] & 255;
            if (codes[c] != 0) {
                if (c == 34) {
                    this._inputPtr = ptr + 1;
                    return this._textBuffer.setCurrentAndReturn(outPtr);
                }
                this._inputPtr = ptr;
                _finishString2(outBuf, outPtr);
                return this._textBuffer.contentsAsString();
            }
            ptr++;
            int outPtr2 = outPtr + 1;
            outBuf[outPtr] = (char) c;
            outPtr = outPtr2;
        }
        this._inputPtr = ptr;
        _finishString2(outBuf, outPtr);
        return this._textBuffer.contentsAsString();
    }

    private final void _finishString2(char[] outBuf, int outPtr) throws IOException {
        int[] codes = _icUTF8;
        byte[] inputBuffer = this._inputBuffer;
        while (true) {
            int ptr = this._inputPtr;
            if (ptr >= this._inputEnd) {
                _loadMoreGuaranteed();
                ptr = this._inputPtr;
            }
            if (outPtr >= outBuf.length) {
                outBuf = this._textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            int max = Math.min(this._inputEnd, (outBuf.length - outPtr) + ptr);
            int ptr2 = ptr;
            int outPtr2 = outPtr;
            while (ptr2 < max) {
                ptr = ptr2 + 1;
                int c = inputBuffer[ptr2] & 255;
                if (codes[c] != 0) {
                    this._inputPtr = ptr;
                    if (c != 34) {
                        switch (codes[c]) {
                            case 1:
                                c = _decodeEscaped();
                                outPtr = outPtr2;
                                break;
                            case 2:
                                c = _decodeUtf8_2(c);
                                outPtr = outPtr2;
                                break;
                            case 3:
                                if (this._inputEnd - this._inputPtr < 2) {
                                    c = _decodeUtf8_3(c);
                                    outPtr = outPtr2;
                                    break;
                                }
                                c = _decodeUtf8_3fast(c);
                                outPtr = outPtr2;
                                break;
                            case 4:
                                c = _decodeUtf8_4(c);
                                outPtr = outPtr2 + 1;
                                outBuf[outPtr2] = (char) (55296 | (c >> 10));
                                if (outPtr >= outBuf.length) {
                                    outBuf = this._textBuffer.finishCurrentSegment();
                                    outPtr = 0;
                                }
                                c = 56320 | (c & 1023);
                                break;
                            default:
                                if (c >= 32) {
                                    _reportInvalidChar(c);
                                    outPtr = outPtr2;
                                    break;
                                }
                                _throwUnquotedSpace(c, "string value");
                                outPtr = outPtr2;
                                break;
                        }
                        if (outPtr >= outBuf.length) {
                            outBuf = this._textBuffer.finishCurrentSegment();
                            outPtr = 0;
                        }
                        outPtr2 = outPtr + 1;
                        outBuf[outPtr] = (char) c;
                        outPtr = outPtr2;
                    } else {
                        this._textBuffer.setCurrentLength(outPtr2);
                        return;
                    }
                }
                outPtr = outPtr2 + 1;
                outBuf[outPtr2] = (char) c;
                ptr2 = ptr;
                outPtr2 = outPtr;
            }
            this._inputPtr = ptr2;
            outPtr = outPtr2;
        }
    }

    protected void _skipString() throws java.io.IOException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxOverflowException: Regions stack size limit reached
	at jadx.core.utils.ErrorsCounter.addError(ErrorsCounter.java:36)
	at jadx.core.utils.ErrorsCounter.methodError(ErrorsCounter.java:60)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
*/
        /*
        r7 = this;
        r6 = 0;
        r7._tokenIncomplete = r6;
        r1 = _icUTF8;
        r2 = r7._inputBuffer;
    L_0x0007:
        r4 = r7._inputPtr;
        r3 = r7._inputEnd;
        if (r4 < r3) goto L_0x004e;
    L_0x000d:
        r7._loadMoreGuaranteed();
        r4 = r7._inputPtr;
        r3 = r7._inputEnd;
        r5 = r4;
    L_0x0015:
        if (r5 >= r3) goto L_0x0036;
    L_0x0017:
        r4 = r5 + 1;
        r6 = r2[r5];
        r0 = r6 & 255;
        r6 = r1[r0];
        if (r6 == 0) goto L_0x004e;
    L_0x0021:
        r7._inputPtr = r4;
        r6 = 34;
        if (r0 == r6) goto L_0x004d;
    L_0x0027:
        r6 = r1[r0];
        switch(r6) {
            case 1: goto L_0x0039;
            case 2: goto L_0x003d;
            case 3: goto L_0x0041;
            case 4: goto L_0x0045;
            default: goto L_0x002c;
        };
    L_0x002c:
        r6 = 32;
        if (r0 >= r6) goto L_0x0049;
    L_0x0030:
        r6 = "string value";
        r7._throwUnquotedSpace(r0, r6);
        goto L_0x0007;
    L_0x0036:
        r7._inputPtr = r5;
        goto L_0x0007;
    L_0x0039:
        r7._decodeEscaped();
        goto L_0x0007;
    L_0x003d:
        r7._skipUtf8_2();
        goto L_0x0007;
    L_0x0041:
        r7._skipUtf8_3();
        goto L_0x0007;
    L_0x0045:
        r7._skipUtf8_4(r0);
        goto L_0x0007;
    L_0x0049:
        r7._reportInvalidChar(r0);
        goto L_0x0007;
    L_0x004d:
        return;
    L_0x004e:
        r5 = r4;
        goto L_0x0015;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.fasterxml.jackson.core.json.UTF8StreamJsonParser._skipString():void");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected com.fasterxml.jackson.core.JsonToken _handleUnexpectedValue(int r5) throws java.io.IOException {
        /*
        r4 = this;
        r1 = 1;
        switch(r5) {
            case 39: goto L_0x0042;
            case 43: goto L_0x0088;
            case 44: goto L_0x002c;
            case 73: goto L_0x006b;
            case 78: goto L_0x004f;
            case 93: goto L_0x0024;
            case 125: goto L_0x003d;
            default: goto L_0x0004;
        };
    L_0x0004:
        r0 = java.lang.Character.isJavaIdentifierStart(r5);
        if (r0 == 0) goto L_0x001d;
    L_0x000a:
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r1 = (char) r5;
        r0 = r0.append(r1);
        r0 = r0.toString();
        r1 = "('true', 'false' or 'null')";
        r4._reportInvalidToken(r0, r1);
    L_0x001d:
        r0 = "expected a valid value (number, String, array, object, 'true', 'false' or 'null')";
        r4._reportUnexpectedChar(r5, r0);
        r0 = 0;
    L_0x0023:
        return r0;
    L_0x0024:
        r0 = r4._parsingContext;
        r0 = r0.inArray();
        if (r0 == 0) goto L_0x0004;
    L_0x002c:
        r0 = com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_MISSING_VALUES;
        r0 = r4.isEnabled(r0);
        if (r0 == 0) goto L_0x003d;
    L_0x0034:
        r0 = r4._inputPtr;
        r0 = r0 + -1;
        r4._inputPtr = r0;
        r0 = com.fasterxml.jackson.core.JsonToken.VALUE_NULL;
        goto L_0x0023;
    L_0x003d:
        r0 = "expected a value";
        r4._reportUnexpectedChar(r5, r0);
    L_0x0042:
        r0 = com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES;
        r0 = r4.isEnabled(r0);
        if (r0 == 0) goto L_0x0004;
    L_0x004a:
        r0 = r4._handleApos();
        goto L_0x0023;
    L_0x004f:
        r0 = "NaN";
        r4._matchToken(r0, r1);
        r0 = com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS;
        r0 = r4.isEnabled(r0);
        if (r0 == 0) goto L_0x0065;
    L_0x005c:
        r0 = "NaN";
        r2 = 9221120237041090560; // 0x7ff8000000000000 float:0.0 double:NaN;
        r0 = r4.resetAsNaN(r0, r2);
        goto L_0x0023;
    L_0x0065:
        r0 = "Non-standard token 'NaN': enable JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS to allow";
        r4._reportError(r0);
        goto L_0x0004;
    L_0x006b:
        r0 = "Infinity";
        r4._matchToken(r0, r1);
        r0 = com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS;
        r0 = r4.isEnabled(r0);
        if (r0 == 0) goto L_0x0081;
    L_0x0078:
        r0 = "Infinity";
        r2 = 9218868437227405312; // 0x7ff0000000000000 float:0.0 double:Infinity;
        r0 = r4.resetAsNaN(r0, r2);
        goto L_0x0023;
    L_0x0081:
        r0 = "Non-standard token 'Infinity': enable JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS to allow";
        r4._reportError(r0);
        goto L_0x0004;
    L_0x0088:
        r0 = r4._inputPtr;
        r1 = r4._inputEnd;
        if (r0 < r1) goto L_0x0099;
    L_0x008e:
        r0 = r4._loadMore();
        if (r0 != 0) goto L_0x0099;
    L_0x0094:
        r0 = com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_INT;
        r4._reportInvalidEOFInValue(r0);
    L_0x0099:
        r0 = r4._inputBuffer;
        r1 = r4._inputPtr;
        r2 = r1 + 1;
        r4._inputPtr = r2;
        r0 = r0[r1];
        r0 = r0 & 255;
        r1 = 0;
        r0 = r4._handleInvalidNumberStart(r0, r1);
        goto L_0x0023;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.fasterxml.jackson.core.json.UTF8StreamJsonParser._handleUnexpectedValue(int):com.fasterxml.jackson.core.JsonToken");
    }

    protected JsonToken _handleApos() throws IOException {
        int outPtr = 0;
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        int[] codes = _icUTF8;
        byte[] inputBuffer = this._inputBuffer;
        while (true) {
            if (this._inputPtr >= this._inputEnd) {
                _loadMoreGuaranteed();
            }
            if (outPtr >= outBuf.length) {
                outBuf = this._textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            int max = this._inputEnd;
            int max2 = this._inputPtr + (outBuf.length - outPtr);
            if (max2 < max) {
                max = max2;
            }
            while (this._inputPtr < max) {
                int i = this._inputPtr;
                this._inputPtr = i + 1;
                int c = inputBuffer[i] & 255;
                int outPtr2;
                if (c != 39 && codes[c] == 0) {
                    outPtr2 = outPtr + 1;
                    outBuf[outPtr] = (char) c;
                    outPtr = outPtr2;
                } else if (c != 39) {
                    switch (codes[c]) {
                        case 1:
                            c = _decodeEscaped();
                            break;
                        case 2:
                            c = _decodeUtf8_2(c);
                            break;
                        case 3:
                            if (this._inputEnd - this._inputPtr < 2) {
                                c = _decodeUtf8_3(c);
                                break;
                            }
                            c = _decodeUtf8_3fast(c);
                            break;
                        case 4:
                            c = _decodeUtf8_4(c);
                            outPtr2 = outPtr + 1;
                            outBuf[outPtr] = (char) (55296 | (c >> 10));
                            if (outPtr2 >= outBuf.length) {
                                outBuf = this._textBuffer.finishCurrentSegment();
                                outPtr = 0;
                            } else {
                                outPtr = outPtr2;
                            }
                            c = 56320 | (c & 1023);
                            break;
                        default:
                            if (c < 32) {
                                _throwUnquotedSpace(c, "string value");
                            }
                            _reportInvalidChar(c);
                            break;
                    }
                    if (outPtr >= outBuf.length) {
                        outBuf = this._textBuffer.finishCurrentSegment();
                        outPtr = 0;
                    }
                    outPtr2 = outPtr + 1;
                    outBuf[outPtr] = (char) c;
                    outPtr = outPtr2;
                } else {
                    this._textBuffer.setCurrentLength(outPtr);
                    return JsonToken.VALUE_STRING;
                }
            }
        }
    }

    protected JsonToken _handleInvalidNumberStart(int ch, boolean neg) throws IOException {
        while (ch == 73) {
            String match;
            if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                _reportInvalidEOFInValue(JsonToken.VALUE_NUMBER_FLOAT);
            }
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            ch = bArr[i];
            if (ch != 78) {
                if (ch != 110) {
                    break;
                }
                match = neg ? "-Infinity" : "+Infinity";
            } else {
                match = neg ? "-INF" : "+INF";
            }
            _matchToken(match, 3);
            if (isEnabled(Feature.ALLOW_NON_NUMERIC_NUMBERS)) {
                return resetAsNaN(match, neg ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
            }
            _reportError("Non-standard token '" + match + "': enable JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS to allow");
        }
        reportUnexpectedNumberChar(ch, "expected digit (0-9) to follow minus sign, for valid numeric value");
        return null;
    }

    protected final void _matchToken(String matchStr, int i) throws IOException {
        int len = matchStr.length();
        if (this._inputPtr + len >= this._inputEnd) {
            _matchToken2(matchStr, i);
            return;
        }
        do {
            if (this._inputBuffer[this._inputPtr] != matchStr.charAt(i)) {
                _reportInvalidToken(matchStr.substring(0, i));
            }
            this._inputPtr++;
            i++;
        } while (i < len);
        int ch = this._inputBuffer[this._inputPtr] & 255;
        if (ch >= 48 && ch != 93 && ch != 125) {
            _checkMatchEnd(matchStr, i, ch);
        }
    }

    private final void _matchToken2(String matchStr, int i) throws IOException {
        int len = matchStr.length();
        do {
            if ((this._inputPtr >= this._inputEnd && !_loadMore()) || this._inputBuffer[this._inputPtr] != matchStr.charAt(i)) {
                _reportInvalidToken(matchStr.substring(0, i));
            }
            this._inputPtr++;
            i++;
        } while (i < len);
        if (this._inputPtr < this._inputEnd || _loadMore()) {
            int ch = this._inputBuffer[this._inputPtr] & 255;
            if (ch >= 48 && ch != 93 && ch != 125) {
                _checkMatchEnd(matchStr, i, ch);
            }
        }
    }

    private final void _checkMatchEnd(String matchStr, int i, int ch) throws IOException {
        if (Character.isJavaIdentifierPart((char) _decodeCharForError(ch))) {
            _reportInvalidToken(matchStr.substring(0, i));
        }
    }

    private final int _skipWS() throws IOException {
        while (this._inputPtr < this._inputEnd) {
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int i2 = bArr[i] & 255;
            if (i2 > 32) {
                if (i2 != 47 && i2 != 35) {
                    return i2;
                }
                this._inputPtr--;
                return _skipWS2();
            } else if (i2 != 32) {
                if (i2 == 10) {
                    this._currInputRow++;
                    this._currInputRowStart = this._inputPtr;
                } else if (i2 == 13) {
                    _skipCR();
                } else if (i2 != 9) {
                    _throwInvalidSpace(i2);
                }
            }
        }
        return _skipWS2();
    }

    private final int _skipWS2() throws IOException {
        int i;
        while (true) {
            if (this._inputPtr < this._inputEnd || _loadMore()) {
                byte[] bArr = this._inputBuffer;
                int i2 = this._inputPtr;
                this._inputPtr = i2 + 1;
                i = bArr[i2] & 255;
                if (i > 32) {
                    if (i == 47) {
                        _skipComment();
                    } else if (i != 35 || !_skipYAMLComment()) {
                        return i;
                    }
                } else if (i != 32) {
                    if (i == 10) {
                        this._currInputRow++;
                        this._currInputRowStart = this._inputPtr;
                    } else if (i == 13) {
                        _skipCR();
                    } else if (i != 9) {
                        _throwInvalidSpace(i);
                    }
                }
            } else {
                throw _constructError("Unexpected end-of-input within/between " + this._parsingContext.typeDesc() + " entries");
            }
        }
        return i;
    }

    private final int _skipWSOrEnd() throws IOException {
        if (this._inputPtr >= this._inputEnd && !_loadMore()) {
            return _eofAsNextChar();
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int i2 = bArr[i] & 255;
        if (i2 <= 32) {
            if (i2 != 32) {
                if (i2 == 10) {
                    this._currInputRow++;
                    this._currInputRowStart = this._inputPtr;
                } else if (i2 == 13) {
                    _skipCR();
                } else if (i2 != 9) {
                    _throwInvalidSpace(i2);
                }
            }
            while (this._inputPtr < this._inputEnd) {
                bArr = this._inputBuffer;
                i = this._inputPtr;
                this._inputPtr = i + 1;
                i2 = bArr[i] & 255;
                if (i2 > 32) {
                    if (i2 != 47 && i2 != 35) {
                        return i2;
                    }
                    this._inputPtr--;
                    return _skipWSOrEnd2();
                } else if (i2 != 32) {
                    if (i2 == 10) {
                        this._currInputRow++;
                        this._currInputRowStart = this._inputPtr;
                    } else if (i2 == 13) {
                        _skipCR();
                    } else if (i2 != 9) {
                        _throwInvalidSpace(i2);
                    }
                }
            }
            return _skipWSOrEnd2();
        } else if (i2 != 47 && i2 != 35) {
            return i2;
        } else {
            this._inputPtr--;
            return _skipWSOrEnd2();
        }
    }

    private final int _skipWSOrEnd2() throws IOException {
        while (true) {
            if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                return _eofAsNextChar();
            }
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int i2 = bArr[i] & 255;
            if (i2 > 32) {
                if (i2 == 47) {
                    _skipComment();
                } else if (i2 != 35 || !_skipYAMLComment()) {
                    return i2;
                }
            } else if (i2 != 32) {
                if (i2 == 10) {
                    this._currInputRow++;
                    this._currInputRowStart = this._inputPtr;
                } else if (i2 == 13) {
                    _skipCR();
                } else if (i2 != 9) {
                    _throwInvalidSpace(i2);
                }
            }
        }
    }

    private final int _skipColon() throws IOException {
        if (this._inputPtr + 4 >= this._inputEnd) {
            return _skipColon2(false);
        }
        int i = this._inputBuffer[this._inputPtr];
        byte[] bArr;
        int i2;
        if (i == 58) {
            bArr = this._inputBuffer;
            i2 = this._inputPtr + 1;
            this._inputPtr = i2;
            i = bArr[i2];
            if (i <= 32) {
                if (i == 32 || i == 9) {
                    bArr = this._inputBuffer;
                    i2 = this._inputPtr + 1;
                    this._inputPtr = i2;
                    i = bArr[i2];
                    if (i > 32) {
                        if (i == 47 || i == 35) {
                            return _skipColon2(true);
                        }
                        this._inputPtr++;
                        return i;
                    }
                }
                return _skipColon2(true);
            } else if (i == 47 || i == 35) {
                return _skipColon2(true);
            } else {
                this._inputPtr++;
                return i;
            }
        }
        if (i == 32 || i == 9) {
            bArr = this._inputBuffer;
            i2 = this._inputPtr + 1;
            this._inputPtr = i2;
            i = bArr[i2];
        }
        if (i != 58) {
            return _skipColon2(false);
        }
        bArr = this._inputBuffer;
        i2 = this._inputPtr + 1;
        this._inputPtr = i2;
        i = bArr[i2];
        if (i <= 32) {
            if (i == 32 || i == 9) {
                bArr = this._inputBuffer;
                i2 = this._inputPtr + 1;
                this._inputPtr = i2;
                i = bArr[i2];
                if (i > 32) {
                    if (i == 47 || i == 35) {
                        return _skipColon2(true);
                    }
                    this._inputPtr++;
                    return i;
                }
            }
            return _skipColon2(true);
        } else if (i == 47 || i == 35) {
            return _skipColon2(true);
        } else {
            this._inputPtr++;
            return i;
        }
    }

    private final int _skipColon2(boolean gotColon) throws IOException {
        while (true) {
            if (this._inputPtr < this._inputEnd || _loadMore()) {
                byte[] bArr = this._inputBuffer;
                int i = this._inputPtr;
                this._inputPtr = i + 1;
                int i2 = bArr[i] & 255;
                if (i2 > 32) {
                    if (i2 == 47) {
                        _skipComment();
                    } else if (i2 != 35 || !_skipYAMLComment()) {
                        if (gotColon) {
                            return i2;
                        }
                        if (i2 != 58) {
                            _reportUnexpectedChar(i2, "was expecting a colon to separate field name and value");
                        }
                        gotColon = true;
                    }
                } else if (i2 != 32) {
                    if (i2 == 10) {
                        this._currInputRow++;
                        this._currInputRowStart = this._inputPtr;
                    } else if (i2 == 13) {
                        _skipCR();
                    } else if (i2 != 9) {
                        _throwInvalidSpace(i2);
                    }
                }
            } else {
                _reportInvalidEOF(" within/between " + this._parsingContext.typeDesc() + " entries", null);
                return -1;
            }
        }
    }

    private final void _skipComment() throws IOException {
        if (!isEnabled(Feature.ALLOW_COMMENTS)) {
            _reportUnexpectedChar(47, "maybe a (non-standard) comment? (not recognized as one since Feature 'ALLOW_COMMENTS' not enabled for parser)");
        }
        if (this._inputPtr >= this._inputEnd && !_loadMore()) {
            _reportInvalidEOF(" in a comment", null);
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int c = bArr[i] & 255;
        if (c == 47) {
            _skipLine();
        } else if (c == 42) {
            _skipCComment();
        } else {
            _reportUnexpectedChar(c, "was expecting either '*' or '/' for a comment");
        }
    }

    private final void _skipCComment() throws IOException {
        int[] codes = CharTypes.getInputCodeComment();
        while (true) {
            if (this._inputPtr < this._inputEnd || _loadMore()) {
                byte[] bArr = this._inputBuffer;
                int i = this._inputPtr;
                this._inputPtr = i + 1;
                int i2 = bArr[i] & 255;
                int code = codes[i2];
                if (code != 0) {
                    switch (code) {
                        case 2:
                            _skipUtf8_2();
                            continue;
                        case 3:
                            _skipUtf8_3();
                            continue;
                        case 4:
                            _skipUtf8_4(i2);
                            continue;
                        case 10:
                            this._currInputRow++;
                            this._currInputRowStart = this._inputPtr;
                            continue;
                        case 13:
                            _skipCR();
                            continue;
                        case R.styleable.ChartTheme_sc_seriesAreaColor3 /*42*/:
                            if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                                break;
                            } else if (this._inputBuffer[this._inputPtr] == (byte) 47) {
                                this._inputPtr++;
                                return;
                            } else {
                                continue;
                            }
                            break;
                        default:
                            _reportInvalidChar(i2);
                            continue;
                    }
                }
            }
            _reportInvalidEOF(" in a comment", null);
            return;
        }
    }

    private final boolean _skipYAMLComment() throws IOException {
        if (!isEnabled(Feature.ALLOW_YAML_COMMENTS)) {
            return false;
        }
        _skipLine();
        return true;
    }

    private final void _skipLine() throws IOException {
        int[] codes = CharTypes.getInputCodeComment();
        while (true) {
            if (this._inputPtr < this._inputEnd || _loadMore()) {
                byte[] bArr = this._inputBuffer;
                int i = this._inputPtr;
                this._inputPtr = i + 1;
                int i2 = bArr[i] & 255;
                int code = codes[i2];
                if (code != 0) {
                    switch (code) {
                        case 2:
                            _skipUtf8_2();
                            break;
                        case 3:
                            _skipUtf8_3();
                            break;
                        case 4:
                            _skipUtf8_4(i2);
                            break;
                        case 10:
                            this._currInputRow++;
                            this._currInputRowStart = this._inputPtr;
                            return;
                        case 13:
                            _skipCR();
                            return;
                        case R.styleable.ChartTheme_sc_seriesAreaColor3 /*42*/:
                            break;
                        default:
                            if (code >= 0) {
                                break;
                            }
                            _reportInvalidChar(i2);
                            break;
                    }
                }
            } else {
                return;
            }
        }
    }

    protected char _decodeEscaped() throws IOException {
        if (this._inputPtr >= this._inputEnd && !_loadMore()) {
            _reportInvalidEOF(" in character escape sequence", JsonToken.VALUE_STRING);
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int c = bArr[i];
        switch (c) {
            case R.styleable.ChartTheme_sc_seriesAreaColor1 /*34*/:
            case R.styleable.ChartTheme_sc_seriesAreaGradientColor4 /*47*/:
            case 92:
                return (char) c;
            case 98:
                return '\b';
            case 102:
                return '\f';
            case 110:
                return '\n';
            case 114:
                return '\r';
            case 116:
                return '\t';
            case 117:
                int value = 0;
                for (int i2 = 0; i2 < 4; i2++) {
                    if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                        _reportInvalidEOF(" in character escape sequence", JsonToken.VALUE_STRING);
                    }
                    bArr = this._inputBuffer;
                    i = this._inputPtr;
                    this._inputPtr = i + 1;
                    int ch = bArr[i];
                    int digit = CharTypes.charToHex(ch);
                    if (digit < 0) {
                        _reportUnexpectedChar(ch, "expected a hex-digit for character escape sequence");
                    }
                    value = (value << 4) | digit;
                }
                return (char) value;
            default:
                return _handleUnrecognizedCharacterEscape((char) _decodeCharForError(c));
        }
    }

    protected int _decodeCharForError(int firstByte) throws IOException {
        int c = firstByte & 255;
        if (c <= 127) {
            return c;
        }
        int needed;
        if ((c & 224) == 192) {
            c &= 31;
            needed = 1;
        } else if ((c & 240) == 224) {
            c &= 15;
            needed = 2;
        } else if ((c & 248) == 240) {
            c &= 7;
            needed = 3;
        } else {
            _reportInvalidInitial(c & 255);
            needed = 1;
        }
        int d = nextByte();
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255);
        }
        c = (c << 6) | (d & 63);
        if (needed <= 1) {
            return c;
        }
        d = nextByte();
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255);
        }
        c = (c << 6) | (d & 63);
        if (needed <= 2) {
            return c;
        }
        d = nextByte();
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255);
        }
        return (c << 6) | (d & 63);
    }

    private final int _decodeUtf8_2(int c) throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int d = bArr[i];
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        return ((c & 31) << 6) | (d & 63);
    }

    private final int _decodeUtf8_3(int c1) throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        c1 &= 15;
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int d = bArr[i];
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        int c = (c1 << 6) | (d & 63);
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        bArr = this._inputBuffer;
        i = this._inputPtr;
        this._inputPtr = i + 1;
        d = bArr[i];
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        return (c << 6) | (d & 63);
    }

    private final int _decodeUtf8_3fast(int c1) throws IOException {
        c1 &= 15;
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int d = bArr[i];
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        int c = (c1 << 6) | (d & 63);
        bArr = this._inputBuffer;
        i = this._inputPtr;
        this._inputPtr = i + 1;
        d = bArr[i];
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        return (c << 6) | (d & 63);
    }

    private final int _decodeUtf8_4(int c) throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int d = bArr[i];
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        c = ((c & 7) << 6) | (d & 63);
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        bArr = this._inputBuffer;
        i = this._inputPtr;
        this._inputPtr = i + 1;
        d = bArr[i];
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        c = (c << 6) | (d & 63);
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        bArr = this._inputBuffer;
        i = this._inputPtr;
        this._inputPtr = i + 1;
        d = bArr[i];
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        return ((c << 6) | (d & 63)) - 65536;
    }

    private final void _skipUtf8_2() throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int c = bArr[i];
        if ((c & 192) != 128) {
            _reportInvalidOther(c & 255, this._inputPtr);
        }
    }

    private final void _skipUtf8_3() throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int c = bArr[i];
        if ((c & 192) != 128) {
            _reportInvalidOther(c & 255, this._inputPtr);
        }
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        bArr = this._inputBuffer;
        i = this._inputPtr;
        this._inputPtr = i + 1;
        c = bArr[i];
        if ((c & 192) != 128) {
            _reportInvalidOther(c & 255, this._inputPtr);
        }
    }

    private final void _skipUtf8_4(int c) throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int d = bArr[i];
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        bArr = this._inputBuffer;
        i = this._inputPtr;
        this._inputPtr = i + 1;
        d = bArr[i];
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        bArr = this._inputBuffer;
        i = this._inputPtr;
        this._inputPtr = i + 1;
        d = bArr[i];
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
    }

    protected final void _skipCR() throws IOException {
        if ((this._inputPtr < this._inputEnd || _loadMore()) && this._inputBuffer[this._inputPtr] == (byte) 10) {
            this._inputPtr++;
        }
        this._currInputRow++;
        this._currInputRowStart = this._inputPtr;
    }

    private int nextByte() throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        return bArr[i] & 255;
    }

    protected void _reportInvalidToken(String matchedPart) throws IOException {
        _reportInvalidToken(matchedPart, "'null', 'true', 'false' or NaN");
    }

    protected void _reportInvalidToken(String matchedPart, String msg) throws IOException {
        StringBuilder sb = new StringBuilder(matchedPart);
        while (true) {
            if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                break;
            }
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            char c = (char) _decodeCharForError(bArr[i]);
            if (!Character.isJavaIdentifierPart(c)) {
                break;
            }
            sb.append(c);
        }
        _reportError("Unrecognized token '" + sb.toString() + "': was expecting " + msg);
    }

    protected void _reportInvalidChar(int c) throws JsonParseException {
        if (c < 32) {
            _throwInvalidSpace(c);
        }
        _reportInvalidInitial(c);
    }

    protected void _reportInvalidInitial(int mask) throws JsonParseException {
        _reportError("Invalid UTF-8 start byte 0x" + Integer.toHexString(mask));
    }

    protected void _reportInvalidOther(int mask) throws JsonParseException {
        _reportError("Invalid UTF-8 middle byte 0x" + Integer.toHexString(mask));
    }

    protected void _reportInvalidOther(int mask, int ptr) throws JsonParseException {
        this._inputPtr = ptr;
        _reportInvalidOther(mask);
    }

    public static int[] growArrayBy(int[] arr, int more) {
        if (arr == null) {
            return new int[more];
        }
        return Arrays.copyOf(arr, arr.length + more);
    }

    protected final byte[] _decodeBase64(Base64Variant b64variant) throws IOException {
        ByteArrayBuilder builder = _getByteArrayBuilder();
        while (true) {
            if (this._inputPtr >= this._inputEnd) {
                _loadMoreGuaranteed();
            }
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int ch = bArr[i] & 255;
            if (ch > 32) {
                int bits = b64variant.decodeBase64Char(ch);
                if (bits < 0) {
                    if (ch == 34) {
                        return builder.toByteArray();
                    }
                    bits = _decodeBase64Escape(b64variant, ch, 0);
                    if (bits < 0) {
                        continue;
                    }
                }
                int decodedData = bits;
                if (this._inputPtr >= this._inputEnd) {
                    _loadMoreGuaranteed();
                }
                bArr = this._inputBuffer;
                i = this._inputPtr;
                this._inputPtr = i + 1;
                ch = bArr[i] & 255;
                bits = b64variant.decodeBase64Char(ch);
                if (bits < 0) {
                    bits = _decodeBase64Escape(b64variant, ch, 1);
                }
                decodedData = (decodedData << 6) | bits;
                if (this._inputPtr >= this._inputEnd) {
                    _loadMoreGuaranteed();
                }
                bArr = this._inputBuffer;
                i = this._inputPtr;
                this._inputPtr = i + 1;
                ch = bArr[i] & 255;
                bits = b64variant.decodeBase64Char(ch);
                if (bits < 0) {
                    if (bits != -2) {
                        if (ch != 34 || b64variant.usesPadding()) {
                            bits = _decodeBase64Escape(b64variant, ch, 2);
                        } else {
                            builder.append(decodedData >> 4);
                            return builder.toByteArray();
                        }
                    }
                    if (bits == -2) {
                        if (this._inputPtr >= this._inputEnd) {
                            _loadMoreGuaranteed();
                        }
                        bArr = this._inputBuffer;
                        i = this._inputPtr;
                        this._inputPtr = i + 1;
                        ch = bArr[i] & 255;
                        if (b64variant.usesPaddingChar(ch)) {
                            builder.append(decodedData >> 4);
                        } else {
                            throw reportInvalidBase64Char(b64variant, ch, 3, "expected padding character '" + b64variant.getPaddingChar() + "'");
                        }
                    }
                }
                decodedData = (decodedData << 6) | bits;
                if (this._inputPtr >= this._inputEnd) {
                    _loadMoreGuaranteed();
                }
                bArr = this._inputBuffer;
                i = this._inputPtr;
                this._inputPtr = i + 1;
                ch = bArr[i] & 255;
                bits = b64variant.decodeBase64Char(ch);
                if (bits < 0) {
                    if (bits != -2) {
                        if (ch != 34 || b64variant.usesPadding()) {
                            bits = _decodeBase64Escape(b64variant, ch, 3);
                        } else {
                            builder.appendTwoBytes(decodedData >> 2);
                            return builder.toByteArray();
                        }
                    }
                    if (bits == -2) {
                        builder.appendTwoBytes(decodedData >> 2);
                    }
                }
                builder.appendThreeBytes((decodedData << 6) | bits);
            }
        }
    }

    public JsonLocation getTokenLocation() {
        Object src = this._ioContext.getSourceReference();
        if (this._currToken == JsonToken.FIELD_NAME) {
            return new JsonLocation(src, this._currInputProcessed + ((long) (this._nameStartOffset - 1)), -1, this._nameStartRow, this._nameStartCol);
        }
        return new JsonLocation(src, this._tokenInputTotal - 1, -1, this._tokenInputRow, this._tokenInputCol);
    }

    public JsonLocation getCurrentLocation() {
        return new JsonLocation(this._ioContext.getSourceReference(), this._currInputProcessed + ((long) this._inputPtr), -1, this._currInputRow, (this._inputPtr - this._currInputRowStart) + 1);
    }

    private final void _updateLocation() {
        this._tokenInputRow = this._currInputRow;
        int ptr = this._inputPtr;
        this._tokenInputTotal = this._currInputProcessed + ((long) ptr);
        this._tokenInputCol = ptr - this._currInputRowStart;
    }

    private final void _updateNameLocation() {
        this._nameStartRow = this._currInputRow;
        int ptr = this._inputPtr;
        this._nameStartOffset = ptr;
        this._nameStartCol = ptr - this._currInputRowStart;
    }

    private static final int pad(int q, int bytes) {
        return bytes == 4 ? q : q | (-1 << (bytes << 3));
    }
}

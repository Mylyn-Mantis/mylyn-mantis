package com.itsolut.mantis.ui.internal.wikitext.block;

import org.eclipse.mylyn.wikitext.core.parser.Attributes;
import org.eclipse.mylyn.wikitext.core.parser.DocumentBuilder.BlockType;
import org.eclipse.mylyn.wikitext.core.parser.markup.Block;


/**
 * @author Robert Munteanu
 *
 */
public class ParagraphBlock extends Block{

    private int blockLineCount = 0;
    
    @Override
    protected int processLineContent(String line, int offset) {

        // start
        if ( blockLineCount == 0 )
            builder.beginBlock(BlockType.PARAGRAPH, new Attributes());
        
        // handle empty lines
        if ( markupLanguage.isEmptyLine(line) ) {
            setClosed(true);
            return 0;
        }
        
        // line break
        if ( blockLineCount != 0 )
            builder.lineBreak();

        // record line
        blockLineCount++;
        
        // emit line
        markupLanguage.emitMarkupLine(getParser(), state, line, offset);
        
        return -1;
    }

    @Override
    public boolean canStart(String line, int lineOffset) {

        return true;
    }
    
    @Override
    public void setClosed(boolean closed) {
    
        if ( closed && !isClosed() )
            builder.endBlock();
        
        super.setClosed(closed);
    }

}

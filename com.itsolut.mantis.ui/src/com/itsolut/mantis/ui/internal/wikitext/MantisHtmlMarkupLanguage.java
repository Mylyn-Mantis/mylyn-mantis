package com.itsolut.mantis.ui.internal.wikitext;

import java.util.List;

import org.eclipse.mylyn.wikitext.core.parser.markup.AbstractMarkupLanguage;
import org.eclipse.mylyn.wikitext.core.parser.markup.Block;
import org.eclipse.mylyn.wikitext.core.parser.markup.phrase.HtmlCommentPhraseModifier;
import org.eclipse.mylyn.wikitext.core.parser.markup.phrase.HtmlEndTagPhraseModifier;
import org.eclipse.mylyn.wikitext.core.parser.markup.phrase.HtmlStartTagPhraseModifier;
import org.eclipse.mylyn.wikitext.core.parser.markup.token.ImpliedHyperlinkReplacementToken;

import com.itsolut.mantis.ui.internal.wikitext.block.ParagraphBlock;


/**
 * @author Robert Munteanu
 *
 */
public class MantisHtmlMarkupLanguage extends AbstractMarkupLanguage {

    public MantisHtmlMarkupLanguage() {
        
        setName("MantisBT HTML");
    }

    @Override
    protected void addStandardTokens(PatternBasedSyntax tokenSyntax) {
     
        tokenSyntax.add(new HtmlEndTagPhraseModifier());
        tokenSyntax.add(new HtmlStartTagPhraseModifier());
        tokenSyntax.add(new ImpliedHyperlinkReplacementToken());
    }

    @Override
    protected void addStandardPhraseModifiers(PatternBasedSyntax phraseModifierSyntax) {
        
        // must add at least one element, otherwise we get a NPE
        phraseModifierSyntax.add(new HtmlCommentPhraseModifier());
    }

    @Override
    protected void addStandardBlocks(List<Block> blocks, List<Block> paragraphBreakingBlocks) {

    }

    @Override
    protected Block createParagraphBlock() {

        return new ParagraphBlock();
    }

}

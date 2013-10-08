package edu.cmu.deiis.annotators;

import java.lang.reflect.Array;
import java.util.ArrayList;

import javax.print.attribute.Size2DSyntax;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.xalan.templates.ElemApplyImport;
import org.omg.CORBA.OBJ_ADAPTER;

import edu.cmu.deiis.types.Answer;
import edu.cmu.deiis.types.AnswerScore;
import edu.cmu.deiis.types.NGram;
import edu.cmu.deiis.types.Question;

/*
 * The fourth step to analyze the artifact.
 * Get AnswerScore annotations from the passed JCas object.
 * Assign a score to each answer using N-Gram(all of the 1-Gram, 2-Gram, 3-Gram) overlap scoring method.
 */
public class AnswerScoreAnnotator extends JCasAnnotator_ImplBase{
  
  public void process(JCas aJCas){
    
    /* Get annotations from the aJCas object passed. */
    AnnotationIndex<Annotation> qIndex = aJCas.getAnnotationIndex(Question.type);
    AnnotationIndex<Annotation> aIndex = aJCas.getAnnotationIndex(Answer.type);    
    AnnotationIndex<Annotation> nIndex = aJCas.getAnnotationIndex(NGram.type);
    FSIndex scnlpTokenIndex = aJCas.getAnnotationIndex(org.cleartk.token.type.Token.type);
    FSIndex scnlpNEIndex = aJCas.getAnnotationIndex(org.cleartk.ne.type.NamedEntityMention.type);
    FSIterator<Annotation> qIterator = qIndex.iterator();
    FSIterator<Annotation> aIterator = aIndex.iterator();
    FSIterator<Annotation> nIterator = nIndex.iterator();
    FSIterator scnlpTokenIterator = scnlpTokenIndex.iterator();
    FSIterator scnlpNEIterator = scnlpNEIndex.iterator();
    
        
    /* get the Question annotation in JCas object */
    Question q = (Question)qIterator.next();
    int qBeginPos = q.getBegin();
    int qEndPos = q.getEnd();
    
    /* Utilize the Scnlp Token type: get all the scnlp Token type annotations in JCas object */
    ArrayList<org.cleartk.token.type.Token> scnlpTokenList = new ArrayList<org.cleartk.token.type.Token>();
    ArrayList<org.cleartk.token.type.Token> qTokenList = new ArrayList<org.cleartk.token.type.Token>();
    
    while(scnlpTokenIterator.hasNext()){
      org.cleartk.token.type.Token token = (org.cleartk.token.type.Token)scnlpTokenIterator.next();
      if((token.getBegin() >= qBeginPos) && (token.getEnd() <= qEndPos)){
        qTokenList.add(token);
      }
      else{
        scnlpTokenList.add(token);
      }
    }
    
    /* Utilize the Scnlp NamedEntityMention annotation type: get all the scnlp Name Entity type annotations in JCas object */
    ArrayList<org.cleartk.ne.type.NamedEntityMention> scnlpNEList = new ArrayList<org.cleartk.ne.type.NamedEntityMention>();
    ArrayList<org.cleartk.ne.type.NamedEntityMention> qNEList = new ArrayList<org.cleartk.ne.type.NamedEntityMention>();
    while(scnlpNEIterator.hasNext()){
      org.cleartk.ne.type.NamedEntityMention ne = (org.cleartk.ne.type.NamedEntityMention)scnlpNEIterator.next();
      if((ne.getBegin() >= qBeginPos) && (ne.getEnd() <= qEndPos)){
        qNEList.add(ne);
      }
      else{
        scnlpNEList.add(ne);
      }      
    }
    
    /* Get all the NGram annotations indexed in JCas object */
    ArrayList<NGram> ngramList = new ArrayList<NGram>();
    while(nIterator.hasNext()){
      ngramList.add((NGram)nIterator.next());
      
    }
    int nGramNum = ngramList.size();  

    /* Get all the NGram annotations in the Question */
    ArrayList<String> qNGramList = new ArrayList<String>();    
    int i;
    for(i = 0;i < nGramNum;i++)
    {
      if((ngramList.get(i).getBegin() >= qBeginPos) && (ngramList.get(i).getEnd() <= qEndPos)){
        qNGramList.add(ngramList.get(i).getCoveredText());
      }
      else {
        break;
      }
    }
       
    
    /* Score each answer. */
    while(aIterator.hasNext()){
      Answer a = (Answer)aIterator.next();
      int aBeginPos = a.getBegin();
      int aEndPos = a.getEnd();
      
      /* Get all the NGram annotations in the Answer */
      ArrayList<String> aNGramList = new ArrayList<String>();
      for(;i < nGramNum;i++){
        if((ngramList.get(i).getBegin() >= aBeginPos) && (ngramList.get(i).getEnd() <= aEndPos)){
          aNGramList.add(ngramList.get(i).getCoveredText());
        }
        else {
          break;
        }
      }
      
      /* Get all the Name Entity annotations in the Answer */
      ArrayList<org.cleartk.ne.type.NamedEntityMention> aNEList = new ArrayList<org.cleartk.ne.type.NamedEntityMention>();
      int neNum = scnlpNEList.size();
      for(int k = 0;k < neNum;k++){
        if((scnlpNEList.get(k).getBegin() >= aBeginPos) && (scnlpNEList.get(k).getEnd() <= aEndPos)){
          aNEList.add(scnlpNEList.get(k));
        }
      }
      
      /* Get all the Scnlp Token annotations in the Answer */
      ArrayList<org.cleartk.token.type.Token> aTokenList = new ArrayList<org.cleartk.token.type.Token>();
      int tokenNum = scnlpTokenList.size();
      for(int k = 0;k < tokenNum;k++){
        if((scnlpTokenList.get(k).getBegin() >= aBeginPos) && (scnlpTokenList.get(k).getEnd() <= aEndPos)){
          aTokenList.add(scnlpTokenList.get(k));
        }
      }
      
      /* calculate the score using NGram overlap method and integrated with additional score  calculated from Name Entity and Token POS Tagging*/
      int qNGramNum = qNGramList.size();
      int qNENum = qNEList.size();
      int qTokenNum = qTokenList.size();
      int aNGramNum = aNGramList.size();
      int aNENum = aNEList.size();
      int aTokenNum = aTokenList.size();
      int aNEResultNum = aNENum;
      int aTokenResultNum = aTokenNum;
      int countNGram = 0;
      int countNE = 0;
      int countScnlpToken = 0;
      for(int j = 0;j < qNGramNum;j++)
      {
        String qNGram_s = qNGramList.get(j);
        for(int k = 0;k < aNGramNum;k++){
          if(aNGramList.get(k).equals(qNGram_s))
          {
            countNGram++;
          }
        }
      }
      
      if(qNENum != 0){
        for (int j = 0; j < qNENum; j++) {
          org.cleartk.ne.type.NamedEntityMention qNE = qNEList.get(j);
          if(aNENum != 0){
            for(int k = 0;k < aNENum;k++){
              if(aNEList.get(k).getMentionType() != null)
              {
                if((aNEList.get(k).getMentionType().equals(qNE.getMentionType())) 
                        && (aNEList.get(k).getCoveredText().equals(qNE.getCoveredText())))
                {
                  countNE++;
                }
              }
              else{
                aNEResultNum--;
              }
            }
          }
        }
      }
      
      if(qTokenNum != 0){
        
      }
      if(qTokenNum != 0){
        for (int j = 0; j < qTokenList.size(); j++) {
          org.cleartk.token.type.Token qToken = qTokenList.get(j);
          if(aTokenNum != 0){
            for(int k = 0;k < aTokenNum;k++){
              if(aTokenList.get(k).getPos() != null){
                if((aTokenList.get(k).getPos().equals(qToken.getPos())) 
                        && (aTokenList.get(k).getLemma().equals(qToken.getLemma())))
                {
                  countScnlpToken++;
                }
              }
              else{
                aTokenResultNum--;
              }
            }
          }

        }
      }

      AnswerScore annotation = new AnswerScore(aJCas);
      annotation.setBegin(a.getBegin());
      annotation.setEnd(a.getEnd());
      annotation.setCasProcessorId("AnswerScore");
      annotation.setConfidence(1);
      annotation.setAnswer(a);
      double neScore,tokenScore;
      if(aNEResultNum != 0){
        neScore = ((double)countNE)/((double)aNENum);
      }
      else {
        neScore = 0;
      }
      
      if(aTokenResultNum != 0){
        tokenScore = ((double)countScnlpToken)/((double)aTokenNum);
      }
      else{
        tokenScore = 0;
      }
      double score = ((double)countNGram)/((double)aNGramNum) + neScore + tokenScore;
      annotation.setScore(score/((double)3));
      annotation.addToIndexes(); /* add the AnswerScore annotations into the JCas index */
    }
    
  }

}
